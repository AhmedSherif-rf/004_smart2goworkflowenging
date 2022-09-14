package com.ntg.engine.jobs;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.entites.ToDoList;
import com.ntg.engine.jobs.service.CommonCachingFunction;
import com.ntg.engine.repository.customImpl.SqlHelperDaoImpl;
import com.ntg.engine.service.JobService;
import com.ntg.engine.util.DatabaseTableNameUtils;
import com.ntg.engine.util.LoginSettings;
import com.ntg.engine.util.LoginUser;
import com.ntg.engine.util.Utils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@DisallowConcurrentExecution
public class EngineMainJob implements Job {

    static boolean IsVersionPosted = false;

    public static volatile List<ToDoList> toDoList = new ArrayList<ToDoList>();

    @Autowired
    private CommonCachingFunction CachingFunction;

    @Autowired
    private JobService jobService;

    @Autowired
    SqlHelperDaoImpl sqlHelperDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${NTG.WFEngine.PendingTasks.ReadThreads.PendingProcessTasks}")
    String PendingProcessTasks;
    @Value("${NTG.WFEngine.PendingTasks.ReadThreads.PendingWorkPackedgeTasks}")
    String PendingWorkPackedgeTasks;
    @Value("${NTG.WFEngine.PendingTasks.ReadThreads.Others}")
    String OthersReadThreads;

    static int CallNo = 0;

    @Value("${postModuleVersion}")
    String postModuleVersionURL;

    @Value("${pom.version}")
    public String backendVersion;

    @Value("${wf_updatePlan}")
    String wf_updatePlanURL;

    @Value("${getSchemaForWorkFlow}")
    String getSchemaForWorkFlow;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LoginSettings loginSettings;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        boolean ShowLifeNotfication = (CallNo == 0 || ((int) ((CallNo / 100)) * 100) == CallNo);
        if (ShowLifeNotfication) {
            JobUtil.Debug("===============================>Main Job Is Life Check #" + (++CallNo)
                    + "<================================");

        }

        if (!IsVersionPosted) {
            try {

                String loginToken = CachingFunction.BackEndLogin(loginSettings.getCompanyName());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
                headers.set("SessionToken", loginToken);
                headers.set("User-Agent", "Smart2GoWorkFlowEngine");

                HttpEntity entity = new HttpEntity<>(void.class, headers);
                String url = this.postModuleVersionURL + "/Smart2Go_Workflow_Engine" + "/" + this.backendVersion;

                ResponseEntity res = restTemplate.exchange(url, HttpMethod.POST, entity,
                        LoginUser.class);
                System.out.println("Post Version --> " + res.getStatusCodeValue());

                IsVersionPosted = true;
            } catch (Exception e) {
                NTGMessageOperation.PrintErrorTrace(e);
            }

        }

        // removed by yghandor this will cuase savethread will never save
        // this is the bug , why the engine was never save !!
        // JobUtil.LastSaveTime = System.currentTimeMillis();

        try {
            DoValidation();

            Thread.sleep(10000);
        } catch (InterruptedException e) {
            NTGMessageOperation.PrintErrorTrace(e);
        }

    }

    private void DoValidation() throws InterruptedException {
        // updatedBy: Aya.Ramadan to stop retreve deleted udas
        String sql = "SELECT O.TABLE_NAME,\n" +
                " O.UDA_TABLE_NAME AS UDATABLE,\n" +
                " U.UDA_NAME,\n" +
                " U.UDA_TABLE_NAME,\n" +
                " U.ADDITIONAL_UDA_TABLE_NAMES,\n" +
                " U.TYPE_ID,\n" +
                " U.COMPANY_NAME\n" +
                "FROM ADM_TYPES_UDA U\n" +
                "JOIN ADM_TYPES T ON T.RECID = U.TYPE_ID\n" +
                "JOIN ADM_OBJECTS O ON O.RECID = T.OBJECTID\n" +
                "JOIN ADM_APPS A ON A.RECID = o.app_id\n" +
                "  \n" +
                "WHERE UDA_TYPE = 18\n" +
                " \n" +
                " AND EXISTS (SELECT 1\n" +
                "   FROM ADM_LICENSE L\n" +
                "   WHERE L.COMPANY_NAME = U.COMPANY_NAME)\n" +
                " AND (A.IS_DELETED = '0'\n" +
                "      OR A.IS_DELETED IS NULL)\n" +
                "      \n" +
                " AND (T.IS_DELETED = '0'\n" +
                "      OR T.IS_DELETED IS NULL) \n" +
                "      \n" +
                " AND(U.IS_DELETED = '0' OR U.IS_DELETED IS NULL)\n" +
                " AND (O.IS_DELETED = '0'\n" +
                "      OR O.IS_DELETED IS NULL)";

        List<Map<String, Object>> PlanUDAList = sqlHelperDao.queryForList(sql);
        if (PlanUDAList != null) {

            for (Map<String, Object> uda : PlanUDAList) {
                String companyName = Utils.isNotEmpty(uda.get("company_name")) ? uda.get("company_name").toString() : "NTG";
                // fetch task tenant schema for engine
                String tenantSchema = fetchSchemaForEachUda(companyName);
                if(JobUtil.AvilableUDAForProcessing.stream().anyMatch(validUda -> validUda.get("tableName").equals(tenantSchema + uda.get("uda_table_name"))))
                    continue;
                String uda_name = (String) uda.get("uda_name");

                String AdditionalUdaTableNames = (String) uda.get("additional_uda_table_names");
                String processTableName = AdditionalUdaTableNames.
                        split(DatabaseTableNameUtils.additionalTablesSeparator)[DatabaseTableNameUtils.processTableIndex];
                String analyticsTableName = AdditionalUdaTableNames.
                        split(DatabaseTableNameUtils.additionalTablesSeparator)[DatabaseTableNameUtils.analyticsTableIndex];

                String[] tableName = new String[2];
                tableName[0] = processTableName;
                tableName[1] = analyticsTableName;

                if (JobUtil.AvilableUDAForProcessingMap.get(companyName + "_" + uda_name) == null) {

                    String[] createPlan = null;

                    for (String t : tableName) {
                        String checkTableExist = null;

                        if (sqlHelperDao.getConnectionType() == 1) {
                            checkTableExist = "SELECT COUNT(*) FROM all_tables WHERE lower(TABLE_NAME) = lower('" + t + "')";
                        } else {
                            checkTableExist = "select count(1) from information_schema.tables where lower(table_name) = lower('" + t + "')";
                        }

                        long tableFound = jdbcTemplate.queryForObject(checkTableExist, Long.class);
                        if (tableFound != 1) {
                            if (t == processTableName) {
                                createPlan = this.sqlHelperDao.BuildCreateTableSql(processTableName,
                                        "recid numeric, object_id numeric, UDA_ID numeric, Type_ID numeric, ParentPLanTaskID numeric , STATUS_ID numeric , no_wait numeric(1) , NUM_OF_RECALL numeric DEFAULT 0"
                                                + ",emai_template_id numeric ,from_mangerial_level numeric , to_mangerial_level numeric , next_approval_if varchar(250), expected_time numeric ",
                                        "recid", tenantSchema);
                            } else if (t == analyticsTableName) {
                                createPlan = this.sqlHelperDao.BuildCreateTableSql(analyticsTableName,
                                        "recid numeric, type_id numeric, task_ID numeric, process_id numeric, task_typeId numeric, task_name varchar(250), "
                                                + "process_name varchar(250), start_time timestamp, end_time timestamp, real_time numeric, expected_time numeric ", "recid", tenantSchema);
                            }

                            for (String plan : createPlan) {
                                jdbcTemplate.execute(plan);
                            }

                        }
                    }
                    DoExecuation(uda, companyName, tenantSchema);
                }
            }
        }

    }


    private void DoExecuation(Map<String, Object> uda, String companyName, String tenantSchema) throws InterruptedException {
        if (uda != null) {
            String uda_name = uda.get("type_id") + "$$" +  (Utils.isNotEmpty(uda.get("uda_name")) ? uda.get("uda_name").toString() : null);
            String Objtable_name = Utils.isNotEmpty(uda.get("table_name")) ? uda.get("table_name").toString() : null;
            HttpHeaders headersRoute = new HttpHeaders();
            headersRoute.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headersRoute.set("SessionToken", CachingFunction.BackEndLogin(companyName));
            headersRoute.set("User-Agent", "Smart2GoWorkFlowEngine");
            HttpEntity<String> entity = new HttpEntity<>(uda_name, headersRoute);
            ResponseEntity responseEntity = restTemplate.exchange(wf_updatePlanURL, HttpMethod.POST, entity, Map.class);

            //JobUtil.Info("*** Pushing available UDA AvilableUDAForProcessingMap <" + uda_name + "> **** ");
            JobDataMap jobData = new JobDataMap(); // JobDataMap data to
            // sent to the job
            String WorkPackedgeTableName = (String) uda.get("uda_table_name");
            String AdditionalUdaTableNames = (String) uda.get("additional_uda_table_names");
            String processTableName = AdditionalUdaTableNames.
                    split(DatabaseTableNameUtils.additionalTablesSeparator)[DatabaseTableNameUtils.processTableIndex];
            String analyticsTableName = AdditionalUdaTableNames.
                    split(DatabaseTableNameUtils.additionalTablesSeparator)[DatabaseTableNameUtils.analyticsTableIndex];
            String escalationHistoryTable = AdditionalUdaTableNames.
                    split(DatabaseTableNameUtils.additionalTablesSeparator)[DatabaseTableNameUtils.escalationHistoryTableIndex];


            jobData.put("analyticsTableName", tenantSchema + analyticsTableName);
            jobData.put("processTableName", tenantSchema + processTableName);
            jobData.put("tableName", tenantSchema + WorkPackedgeTableName);
            jobData.put("escHistoryTableName", tenantSchema + escalationHistoryTable);
            jobData.put("type_id", uda.get("type_id"));
            jobData.put("uda", uda);
            jobData.put("Objtable_name", tenantSchema + Objtable_name);
            jobData.put("companyName", companyName);
            jobData.put("tenantSchema", tenantSchema);

            JobUtil.AvilableUDAForProcessing.add(jobData);
            JobUtil.AvilableUDAForProcessingMap.put(companyName + "_" + uda_name, true);

        }

        // Start Save Thread if it is not running
        if (!jobService.isJobWithNamePresent("saveThread")) {
            jobService.scheduleOneTimeJob("saveThread", JobUtil.getJobClassByName("saveThread"), new Date(),
                    new JobDataMap());
        }

        // Starting the read Threads
        int AvilableUDAs = JobUtil.AvilableUDAForProcessing.size();

        int PendingProcessTasks = Integer.valueOf(this.PendingProcessTasks);
        int PendingWorkPackedgeTasks = Integer.valueOf(this.PendingWorkPackedgeTasks);
        int OthersReadThreads = Integer.valueOf(this.OthersReadThreads);


        int NThread1 = (PendingProcessTasks > AvilableUDAs) ? AvilableUDAs : PendingProcessTasks;
        int NThread2 = (PendingWorkPackedgeTasks > AvilableUDAs) ? AvilableUDAs : PendingWorkPackedgeTasks;
        int NThread3 = (OthersReadThreads > AvilableUDAs) ? AvilableUDAs : OthersReadThreads;

        for (int i = 0; i < NThread1; i++) {
            String ThreadName = "PendingProcessTasksReadingThread#" + i;

            if (jobService.isJobRunning(ThreadName) == false) {


                jobService.scheduleOneTimeJob(ThreadName, JobUtil.getJobClassByName("PendingTaskHandler"), new Date(),
                        null);
            }
        }
        for (int i = 0; i < NThread2; i++) {
            String ThreadName = "PendingWorkPackedgeTasksReadingThread#" + i;

            if (jobService.isJobRunning(ThreadName) == false) {

                jobService.scheduleOneTimeJob(ThreadName, JobUtil.getJobClassByName("PendingTaskHandler"), new Date(),
                        null);
            }
        }
        for (int i = 0; i < NThread3; i++) {
            String ThreadName = "OthersReadingThread#" + i;

            if (jobService.isJobRunning(ThreadName) == false) {

                jobService.scheduleOneTimeJob(ThreadName, JobUtil.getJobClassByName("PendingTaskHandler"), new Date(),
                        null);
            }
        }

    }


    private String fetchSchemaForEachUda(String companyName) throws InterruptedException {
        String tenantSchema = (Utils.isNotEmpty(companyName)) ? CachingFunction.getTenantSchemaName(companyName) : null;
        if (Utils.isEmpty(tenantSchema)) {
            String loginToken = CachingFunction.BackEndLogin(loginSettings.getCompanyName());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headers.set("SessionToken", loginToken);
            headers.set("User-Agent", "Smart2GoWorkFlowEngine");

            HttpEntity entity = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(getSchemaForWorkFlow, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {
            });
            if (Utils.isNotEmpty(res) && Utils.isNotEmpty(res.getBody())) {
                Map<String, Object> schemas = res.getBody();
                Set<String> keys = schemas.keySet();
                for (String key : keys) {
                    CachingFunction.cacheTenantSchemaName(key, (Utils.isNotEmpty(schemas.get(key))) ? schemas.get(key).toString() : "");
                }
            }
            tenantSchema = CachingFunction.getTenantSchemaName(companyName);
        }
        if (Utils.isEmpty(tenantSchema) || tenantSchema.equals(".")) {
            tenantSchema = "";
        } else {
            tenantSchema += ".";
        }
        return tenantSchema;
    }

}
