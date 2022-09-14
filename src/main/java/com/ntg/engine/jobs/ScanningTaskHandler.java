package com.ntg.engine.jobs;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.repository.customImpl.SqlHelperDaoImpl;
import com.ntg.engine.service.JobService;
import com.ntg.engine.util.Setting;
import com.ntg.engine.util.Utils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ScanningTaskHandler implements Job {

    @Autowired
    private SqlHelperDaoImpl sqlHelperDao;

    @Autowired
    private JobService jobService;


    private JobDataMap jobData;

    String jobName;
    long NoOfProcessedFoundData = 0;
    long NoOfIgnoredRows = 0;
    public String tableName = "";
    public String processTableName = "";
    public String analyticsTableName = "";
    public String ObjTableName = "";
    public String escHistoryTableName = "";
    public Long typeID = null;
    public String companyName;
    public String tenantSchema;

    @Value("${NTG.WFEngine.PendingTasks.ProcessingThreads}")
    String NProcessingThreads;

    @Value("${NTG.WFEngine.TaskEscalation.EscalationReadThreads}")
    String EscalationReadThreads;

    @Value("${NTG.WFEngine.PendingTasks.pmpprocesstasks}")
    String ReadThreads;

    @Value("${NTG.WFEngine.PendingTasks.pmpswitchtasks}")
    String ReadSwitchTaskThreads;

    @Value("${NTG.WFEngine.PendingTasks.expiryTasks.ProcessingThreads}")
    String expiryTasks_ProcessingThreads;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        jobName = context.getJobDetail().getKey().toString().split("\\.")[1];
        int ThreadNUmber = (jobName.startsWith("PendingProcess")) ? 0 :
                (jobName.startsWith("PendingWorkPackedge")) ? 1 : 2;
        //JobUtil.Debug("==============Start PendingTaskHandler ============== <" + jobName + ">");
        try {

            jobData = JobUtil.getOneJobDataMap(ThreadNUmber);
            while (jobData != null) {
                NoOfProcessedFoundData = 0;
                NoOfIgnoredRows = 0;
                tableName = jobData.getString("tableName");
                ObjTableName = jobData.getString("Objtable_name");
                processTableName = jobData.getString("processTableName");
                analyticsTableName = jobData.getString("analyticsTableName");
                escHistoryTableName = jobData.getString("escHistoryTableName");
                typeID = Long.valueOf(jobData.get("type_id") + "");
                companyName = jobData.getString("companyName");
                tenantSchema = jobData.getString("tenantSchema");

                StartTableScanning();


                if (NoOfProcessedFoundData == 0) {
                    // no data have bean read
                    // Sleep for second
                    if (SaveThread.saveThreadPool.size() > 0) {
                        JobUtil.ForceSaveThreadForSave();
                    }
                    Thread.sleep(Setting.SleepPeriodOnNoDataFound);
                }
                JobUtil.FreeJob(jobData,ThreadNUmber);
                jobData = null;

                // JobUtil.Debug("==============<" + jobName + "> Free Processing -> " + tableName);

                jobData = JobUtil.getOneJobDataMap(ThreadNUmber);
            }
        } catch (Exception e) {
            if (jobData != null) {
                JobUtil.FreeJob(jobData,ThreadNUmber);
                jobData = null;
            }
            NTGMessageOperation.PrintErrorTrace(e);
        }

        jobService.deleteJob(jobName);
        //JobUtil.Debug("==============End PendingTaskHandler ============== <" + jobName + ">");

    }

    private void StartTableScanning() {

        // scan workPackage tables case of (pm_table)
        if (jobName.startsWith("PendingWorkPackedgeTasksReadingThread")) {
            workPacageTable();
            JobUtil.Debug("->" + jobName.replaceAll("PendingWorkPackedgeTasksReadingThread", "WP") + ": Tenent Name: " + companyName + " -->" + "," + tableName
                    + "|Scanned->" + NoOfProcessedFoundData + " Found," + NoOfIgnoredRows + " Ignored,(P:" + (JobUtil.PendingTasksPool.size() + JobUtil.PendingSwitchConditionTasksPool.size() + JobUtil.PendingProcessTaskPool.size() + JobUtil.PendingTaskEscalationsPool.size())
                    + ",U:" + (JobUtil.UnderProcessingPool.size() + JobUtil.UnderProcessingSwitchConditionPool.size() + JobUtil.UnderProcessingProcessTaskPool.size() + JobUtil.UnderProcessingEscalationPool.size())
                    + ",C:" + (JobUtil.CompletedTasksPool.size() + JobUtil.CompletedTasksPool.size() + JobUtil.CompletedTasksPool.size()) + ")");


        } else if (jobName.startsWith("PendingProcessTasksReadingThread")) {
            // scan process table (pmp_table) to change the status of every task
            processTable();
            JobUtil.Debug("->" + jobName.replaceAll("PendingProcessTasksReadingThread", "Pr") + ": Tenent Name: " + companyName + " -->" + processTableName

                    + "|Scanned->" + NoOfProcessedFoundData + " Found," + NoOfIgnoredRows + " Ignored,(P:"
                    + (JobUtil.PendingTasksPool.size() + JobUtil.PendingSwitchConditionTasksPool.size() + JobUtil.PendingProcessTaskPool.size() + JobUtil.PendingTaskEscalationsPool.size())

                    + ",U:" + (JobUtil.UnderProcessingPool.size() + JobUtil.UnderProcessingSwitchConditionPool.size() + JobUtil.UnderProcessingProcessTaskPool.size() + JobUtil.UnderProcessingEscalationPool.size())

                    + ",C:" + (JobUtil.CompletedTasksPool.size() + JobUtil.CompletedTasksPool.size() + JobUtil.CompletedTasksPool.size()) + ")");


        } else if (jobName.startsWith("OthersReadingThread")) {

            // Scan Process Merge Tasks
            scanningMergeTasks();

            // Scan EscalationInfo table
            escalationInfoTable();

            // added to Start switch task & process Task Threads
            CheckSwitchAndProcessProcessingThread();


            //Scann Expiry Tasks

            ScanExpiredTasks();

            JobUtil.Debug("->" + jobName.replaceAll("ReadingThread", "") + ": Tenent Name: " + companyName + " -->" + ObjTableName
                    + "|Scanned->" + NoOfProcessedFoundData + " Found," + NoOfIgnoredRows + " Ignored,(P:" + (JobUtil.PendingTasksPool.size() + JobUtil.PendingSwitchConditionTasksPool.size() + JobUtil.PendingProcessTaskPool.size() + JobUtil.PendingTaskEscalationsPool.size())
                    + ",U:" + (JobUtil.UnderProcessingPool.size() + JobUtil.UnderProcessingSwitchConditionPool.size() + JobUtil.UnderProcessingProcessTaskPool.size() + JobUtil.UnderProcessingEscalationPool.size())
                    + ",C:" + (JobUtil.CompletedTasksPool.size() + JobUtil.CompletedTasksPool.size() + JobUtil.CompletedTasksPool.size())

                    + ",exP:" + JobUtil.ExpiredTasks.size() + ",exU:" + JobUtil.UnderProcessingExpiredTasks.size() + ")");

        }

        /// Calculate N Thread to be started for processing
        int AvilableData = JobUtil.PendingTasksPool.size();
        int MaxThreads = Integer.valueOf(NProcessingThreads);
        int n = (AvilableData < MaxThreads) ? AvilableData : MaxThreads;
        // This Function will check the Processing Threads for Work Package
        // and process human tasks only
        CheckProcesssingThread(n, jobService);

    }

    private void ScanExpiredTasks() {

        String Sql = "select t.plannedenddate,t.expiration_action,a.* from sa_Todo_List a join pm_task t on t.recid= a.task_ID\n" +
                "where expiration_action is not null and a.expiration_date < localtimestamp " +
                " And a.uda_name = '" + processTableName.replaceAll(tenantSchema, "") + "'";
        ;

        List<Map<String, Object>> udaTasks = sqlHelperDao.queryForList(Sql);

        if (Utils.isNotEmpty(udaTasks)) {
            boolean NoDataFound = true;
            for (Map<String, Object> task : udaTasks) {
                long taskrecid = JobUtil.convertBigDecimalToLong(task.get("recid"));
                String key = "e" + taskrecid;
                if (JobUtil.ExpiredTasks.get(key) == null && JobUtil.UnderProcessingExpiredTasks.get(key) == null) {
                    JobUtil.ExpiredTasks.put(key, task);
                    NoDataFound = false;
                }
            }
            if (NoDataFound == false) {
                StartExpiryTasksThreads();
            }

        }

    }

    private void StartExpiryTasksThreads() {

        int nThread = Integer.valueOf(expiryTasks_ProcessingThreads);
        int nAvilableTasks = JobUtil.ExpiredTasks.size();

        int NUmberOFThreadToStart = (nAvilableTasks > nThread) ? nThread : nAvilableTasks;


        for (int i = 0; i < NUmberOFThreadToStart; i++) {
            String ThreadName = "ExpiryTaskHandler#" + i;

            if (jobService.isJobRunning(ThreadName) == false) {
                jobService.scheduleOneTimeJob(ThreadName, JobUtil.getJobClassByName("expiryTaskHandler"),
                        new Date(), null);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                }
            }
        }


    }

    @SuppressWarnings("unchecked")
    private static synchronized void CheckProcesssingThread(int NUmberOFThreadToStart, JobService jobService) {
        for (int i = 0; i < NUmberOFThreadToStart; i++) {
            String ThreadName = "PendingTaskProcessingThread#" + i;

            if (jobService.isJobRunning(ThreadName) == false) {
                jobService.scheduleOneTimeJob(ThreadName, JobUtil.getJobClassByName("PendingTaskProcessing"),
                        new Date(), null);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    private void workPacageTable() {
        try {
            // This Sql Fetch Tasks from Plan which meet the flowing criteria
            // 1- actualstartdate is null which mean the task not started
            // 2- wp.task_type_id in (1,2) for WorkPackedge and process only
            // 3- Have Valid Assignment if it is workPackage
            // 4 wp.PLANNEDSTARTDATE <= localtimestamp
            String getTasksSQL = "SELECT o.created_by_id , o.created_by,"
                    + "wp.recid taskId, wp.CREATEDBYID createdById, wp.ASSIGNEE assigneeName, wp.CCLIST cclist , "
                    + "wp.OBJECT_ID objectId, wp.URGENTID priortyId, NULL PARENTSEQUANCE , wp.prime_id primeId, "
                    + "wp.prime_type_id primeTypeId, p.recid ticketId, wp.task_type_id  tasktypeid , wp.planned_efforts plannedEfforts, "
                    + "duration , wp.PROCESS_ID , wp.PLANNEDSTARTDATE plannedStartDate, coalesce(wp.description,wp.taskname) description, "
                    + "wp.taskname taskName, NULL processtobestarted, wp.PLANNEDENDDATE plannedEndDate FROM " + tableName

                    + " wp join " + ObjTableName + " o on o.recid = wp.object_id JOIN adm_types p ON p.recid = " + typeID
                    + " WHERE (o.is_deleted is null OR o.is_deleted = '0') and wp.task_type_id in (1,2) AND wp.actualstartdate IS NULL "
                    + " and ( ( prime_type_id IN (4,6,7) "
                    + "OR ( prime_type_id IN (0,1,2,3,4,5) AND wp.prime_id > 0 ) )  OR wp.task_type_id  in (2) ) AND (wp.PLANNEDSTARTDATE <= localtimestamp or  wp.no_wait =1) "
                    + "AND ( wp.parent_workpackages IS NULL  OR TRIM(wp.parent_workpackages) = ''  OR ( ( SELECT "
                    + "CASE WHEN MIN(CASE  WHEN pwp.actualenddate IS NULL THEN -1  ELSE 1 END) = MAX(CASE  WHEN pwp.actualenddate IS NULL THEN 0 ELSE 1 "
                    + " END) THEN 1 ELSE 0 END finalparentstatus  FROM  " + tableName
                    + " pwp WHERE ','|| wp.parent_workpackages|| ',' LIKE '%,'|| pwp.task_seq|| ',%' and pwp.Object_ID = wp.Object_ID) = 1 ) )";
            /*
             * adding filter and pwp.Object_ID = wp.Object_ID to be sure the predecessors is
             * from same object (bug Fixing)
             */

            List<Map<String, Object>> udaTasks = sqlHelperDao.queryForList(getTasksSQL);
            if (Utils.isNotEmpty(udaTasks)) {
                for (Map<String, Object> task : udaTasks) {

                    task.put("tableType", "workPackage");
                    RegisterTheTask(task);
                }
            }
        } catch (Exception e) {
            JobUtil.Debug("War:Fail to Scan WPK:" + tableName + " For " + ObjTableName + " due " + e.getMessage());
        }
    }

    private void processTable() {
        // AND( p.PRIME_ID is not NULL)
        // Find task from process which have pending status p.STATUS_ID = 2
        //yghandor adding task 11 <Start> to route to next tasks
        //Types1 --> Human Task,
        // 4 --> uestion Task,
        // 5 --> Dummy Task,
        // 6 --> Finish Task,
        // 8 --> Process Task,
        // 7 --> Switch Case Task,
        // 2 --> Conditional Task,
        // 9 --> sub-process Task,
        // 10 --> Email Task
        // 11 -->  Start Task
        //yghandor add fetch start task even if the task is created by modfay status check to (p.STATUS_ID = 2 or (p.STATUS_ID =1 and p.TASKTYPEID=11))
        //t_sub_planid changed to get it from Task Table not from the copy
        try {
            String getTasksSQL = "SELECT t.sub_status_taskId, t.inherit_All_Rules ,t.expiration_days,t.is_expiry_task,t.company_name,tt.objectid,t.sub_planid t_sub_planid,o.created_by , o.created_by_id ,t.assignment_uda_id tassignment_uda_id ," +
                    "p.*,t.description tdescription,t.durationuda,t.priorityuda,t.SEND_MAIL_ON_ASSIGN,p.reset_date_every_reassign resetAssignDate FROM "
                    + processTableName + " p  join " + ObjTableName
                    + " o on o.recid = p.object_id join PM_TASK t on t.recid = p.TASKID " +
                    " join adm_types tt on tt.recid = p.Type_ID" +
                    " WHERE (o.is_deleted is null OR o.is_deleted = '0') and (p.STATUS_ID = 2 or (p.STATUS_ID =1 and p.TASKTYPEID=11)) AND p.TASKTYPEID IN (1,4,5,6,8,7,2,9,10,11) AND (p.minstartdate <= localtimestamp or p.minstartdate is null) AND( p.PRIME_ID is not NULL or p.TASKTYPEID IN (5,6,8,7,2,9,10,11)) ";
            List<Map<String, Object>> udaTasks = sqlHelperDao.queryForList(getTasksSQL);
            if (Utils.isNotEmpty(udaTasks)) {
                for (Map<String, Object> task : udaTasks) {
                    task.put("tableType", "process");
                    RegisterTheTask(task);
                }
            } else {
                //yghandor Dev-00000466
                //run Update Sql to convert tasks to pending from assign if Sa_TodoList is not found
                String updateSql = "update " + processTableName + " a set status_ID=2\n" +
                        "where  status_id = 3 and not exists (\n" +
                        "select * from sa_ToDO_List t where  t.incom_recID = a.recid\n" +
                        "and uda_name = '" + processTableName.replaceAll(tenantSchema, "") + "')\n";

                this.sqlHelperDao.RunUpdateSql(updateSql);
            }
        } catch (Exception e) {
            JobUtil.Debug("War:Fail to Scan Pro:" + processTableName + " For " + ObjTableName + " due " + e.getMessage());
        }
    }

    private void scanningMergeTasks() {
        try {
            String mergeTaskSql = " WITH mergedependancystatusrecords AS (  SELECT t.recid  mergetaskid ,"
                    + " tt.recid WaitedTaskID , MAX(CASE WHEN tt.status_id = r.status_id THEN 1 ELSE 0 END) matchstatusforeachtask "
                    + " FROM " + processTableName + " t JOIN pm_task_routing r ON r.to_taskid = t.taskid JOIN "
                    + processTableName + " tt ON tt.taskid = r.from_taskid  AND tt.object_id = t.object_id  WHERE "
                    + " t.tasktypeid = 3 AND t.status_id IN ( 1, 2)  GROUP BY  tt.recid,t.recid) ,"
                    + "	FinalMergeSql as (	SELECT mergetaskid , sum(matchstatusforeachtask) NumberOfTaskHaveClearDependncy ,"
                    + " count(1) NumberWaitingOfTasks FROM  mergedependancystatusrecords GROUP BY mergetaskid "
                    + " Having sum(matchstatusforeachtask) = count(1)  ) SELECT t.* FROM FinalMergeSql main JOIN "
                    + processTableName + " t ON t.recid = main.mergetaskid ";

            List<Map<String, Object>> mergeTasks = sqlHelperDao.queryForList(mergeTaskSql);
            if (Utils.isNotEmpty(mergeTasks)) {
                for (Map<String, Object> mergeTask : mergeTasks) {
                    mergeTask.put("tableType", "process");
                    RegisterTheTask(mergeTask);
                }
            }
        } catch (Exception e) {
            JobUtil.Debug("War:Fail to Scan Merg:" + processTableName + " For " + ObjTableName + " due " + e.getMessage());

        }
    }

    @SuppressWarnings("unchecked")
    private void escalationInfoTable() {
        try {
            String getEscalationSql = null;
            DateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            if (sqlHelperDao.getConnectionType() == 1) {

                getEscalationSql = "SELECT p.*, a.recid plantaskid, TO_DATE('" + dateTimeFormatter.format(new Date())
                        + "','YYYY-MM-DD HH24:MI:SS')- TO_DATE(TO_CHAR(a.reqcomplationdate, 'YYYY-MM-DD HH24:MI:SS'), 'YYYY-MM-DD HH24:MI:SS') AS latedate,"
                        + " a.object_id,o.created_by_id, a.actualenddate, a.recid  FROM " + processTableName + " a "
                        + "JOIN " + ObjTableName + " o ON o.recid = a.object_id and a.status_id = 3  , pm_escalation_info p , pm_task t \n"
                        + "WHERE (o.is_deleted is null OR o.is_deleted = '0') and a.taskid = p.pm_task_id AND p.pm_task_id = t.recid\n"
                        + "AND a.actualenddate IS  NULL AND TO_DATE('" + dateTimeFormatter.format(new Date())
                        + "','YYYY-MM-DD HH24:MI:SS')- TO_DATE(TO_CHAR(a.reqcomplationdate, 'YYYY-MM-DD HH24:MI:SS'), 'YYYY-MM-DD HH24:MI:SS') >= \n"
                        + "\n" + "(  CASE WHEN (p.interval_type_id = 1) THEN (p.interval) \n" + "ELSE\n"
                        + "(p.interval /(60*24))\n" + "end)" + " AND NOT EXISTS ( SELECT 1 FROM " + escHistoryTableName
                        + " c " + " WHERE c.task_id = a.recid AND c.escalation_id = p.recid )";


            } else {

                getEscalationSql = "SELECT p.*, a.recid plantaskid ,case p.interval_type_id"
                        + " when 1 then( extract (epoch from (localtimestamp)) - extract (epoch from (a.reqcomplationdate::timestamp )))/(60*60*24)"
                        + " when 2 then( extract (epoch from (localtimestamp)) - extract (epoch from (a.reqcomplationdate::timestamp )))/(60*60)"
                        + " end AS latedate, a.object_id,o.created_by_id FROM " + processTableName + " a  JOIN "
                        + ObjTableName + " o ON o.recid = a.object_id and a.status_id = 3 ," + " pm_escalation_info p , pm_task t "
                        + " WHERE (o.is_deleted is null OR o.is_deleted = '0') and a.taskid = p.pm_task_id AND p.pm_task_id = t.recid AND a.actualenddate IS  NULL AND "
                        + "	( extract (epoch from (localtimestamp)) - extract (epoch from (a.reqcomplationdate::timestamp )))/(60*60*24) >= "
                        + " (  CASE WHEN (p.interval_type_id = 1) THEN (p.interval) \n" + "ELSE\n"
                        + "(p.interval /(60*24)::Float)\n" + "end)" + " AND NOT EXISTS ( SELECT 1 FROM " + escHistoryTableName
                        + " c " + " WHERE c.task_id = a.recid AND c.escalation_id = p.recid )";
            }

            List<Map<String, Object>> taskEscalations = sqlHelperDao.queryForList(getEscalationSql);

            if (Utils.isNotEmpty(taskEscalations)) {
                for (Map<String, Object> escalation : taskEscalations) {
                    escalation.put("tableType", "process");
                    RegisterEscalationTask(escalation);

                }

                // Starting escalation read Threads
                int AvilableEscalations = JobUtil.PendingTaskEscalationsPool.size();
                int EscalationReadThreads = Integer.valueOf(this.EscalationReadThreads);
                int NEscThread = (EscalationReadThreads > AvilableEscalations) ? AvilableEscalations
                        : EscalationReadThreads;

                for (int i = 0; i < NEscThread; i++) {
                    String ThreadName = "PendingTaskEscalationThread#" + i;
                    if (jobService.isJobRunning(ThreadName) == false) {

                        jobService.scheduleOneTimeJob(ThreadName, JobUtil.getJobClassByName("PendingTaskEscalation"),
                                new Date(), null);
                    }
                }

            }
        } catch (Exception e) {
            JobUtil.Debug("War:Fail to Scan Esc:" + processTableName + " For " + ObjTableName + " due " + e.getMessage());

        }
    }

    private void RegisterTheTask(Map<String, Object> task) {
        task.put("tableName", tableName);
        task.put("processTableName", processTableName);
        task.put("analyticsTableName", analyticsTableName);
        task.put("uda", jobData.get("uda"));
        task.put("companyName", companyName);
        task.put("tenantSchema", tenantSchema);

        if (Utils.isNotEmpty(task.get("tableType")) && task.get("tableType").toString().equals("process")) {
            if (Utils.isNotEmpty(task.get("tasktypeid"))) {
                Long taskTypeId = Long.parseLong(task.get("tasktypeid").toString());
                if (taskTypeId == 8) {

                    // set process tasks in it pool
                    RegisterProcessTask(task);
                } else if (taskTypeId == 7 || taskTypeId == 2) {
                    // set switch and condition tasks in it pool
                    RegisterSwitchAndConditionTask(task);
                } else {
                    // register task from process human tasks
                    RegisterHumanTask(task);
                }
            }
        } else {
            // register task from plan workPackge task
            RegisterHumanTask(task);
        }

    }

    private void RegisterHumanTask(Map<String, Object> task) {
        String TaskKey = JobUtil.CreatePendingTaskKey(task);

        if (JobUtil.CompletedTasksPool.IsExist(TaskKey) != null) {
            NoOfIgnoredRows++;
            //JobUtil.Debug("<" + jobName + "> Ignore Task : " + TaskKey + "Found In CompletedTasksPool");
        } else if (JobUtil.UnderProcessingPool.IsExist(TaskKey) != null) {
            NoOfIgnoredRows++;
            // JobUtil.Debug("<" + jobName + "> Ignore Task : " + TaskKey + "Found In UnderProcessingPool");
        } else if (JobUtil.PendingTasksPool.get(TaskKey) != null) {
            NoOfIgnoredRows++;
            // JobUtil.Debug("<" + jobName + "> Ignore Task : " + TaskKey + "Found In PendingTasksPool");
        } else {
            Long taskTypeId = Long.parseLong(task.get("tasktypeid").toString());
            if (taskTypeId == 1) {
                Long recid = sqlHelperDao.FtechRecID(task.get("analyticsTableName") + "_s");

                Date startDate = new Date();

                if (sqlHelperDao.getConnectionType() > 1)
                    startDate = new Timestamp(System.currentTimeMillis());

                String sql2 = "insert into " + task.get("analyticsTableName") + "(" + "RECID ," + "TASK_ID ,"
                        + "TASK_TYPEID ," + "TASK_NAME ," + "START_TIME, " + "EXPECTED_TIME, " + "TYPE_ID, "
                        + "PROCESS_ID " + ") " + "values (?,?,?,?,?,?, ?,?)";

                List<SaveThreadTransactionInformation> txs = new ArrayList<>();
                ArrayList<Object> pram = new ArrayList<>();
                pram.add(recid);
                pram.add(task.get("recid"));
                pram.add(task.get("tasktypeid"));
                pram.add(task.get("name"));
                pram.add(startDate);
                pram.add(task.get("expected_time"));
                pram.add(task.get("type_id"));
                pram.add(task.get("object_id"));
                SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(sql2,
                        SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);

                // added by Mahmoud for check in
                tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;

                txs.add(tx);
                SaveThread.AddSaveThreadTransaction(txs);

                task.put("analytics_recid", recid);

            }

            NoOfProcessedFoundData++;
            JobUtil.PendingTasksPool.put(TaskKey, task);
        }

    }

    private void RegisterSwitchAndConditionTask(Map<String, Object> task) {

        // create task key
        String TaskKey = JobUtil.CreateSwitchConditionTasksKey(task);

        // fill task in the right pool
        if (JobUtil.CompletedTasksPool.IsExist(TaskKey) != null) {
            NoOfIgnoredRows++;
            // JobUtil.Debug("<" + jobName + "> Ignore Task : " + TaskKey + "Found In CompletedTasksPool");
        } else if (JobUtil.UnderProcessingSwitchConditionPool.get(TaskKey) != null) {
            NoOfIgnoredRows++;
            // JobUtil.Debug("<" + jobName + "> Ignore Task : " + TaskKey + "Found In UnderProcessingSwitchConditionPool");
        } else if (JobUtil.PendingSwitchConditionTasksPool.get(TaskKey) != null) {
            NoOfIgnoredRows++;
            //  JobUtil.Debug("<" + jobName + "> Ignore Task : " + TaskKey + "Found In PendingSwitchConditionTasksPool");
        } else {
            NoOfProcessedFoundData++;
            JobUtil.PendingSwitchConditionTasksPool.put(TaskKey, task);
        }
    }

    private void RegisterProcessTask(Map<String, Object> task) {

        // create task key
        String TaskKey = JobUtil.CreateProcessTasksKey(task);

        // fill task in the right pool
        if (JobUtil.CompletedTasksPool.IsExist(TaskKey) != null) {
            NoOfIgnoredRows++;
            // JobUtil.Debug("<" + jobName + "> Ignore Task : " + TaskKey + "Found In CompletedTasksPool");
        } else if (JobUtil.UnderProcessingProcessTaskPool.get(TaskKey) != null) {
            NoOfIgnoredRows++;
            //  JobUtil.Debug("<" + jobName + "> Ignore Task : " + TaskKey + "Found In UnderProcessingProcessTaskPool");
        } else if (JobUtil.PendingProcessTaskPool.get(TaskKey) != null) {
            NoOfIgnoredRows++;
            //  JobUtil.Debug("<" + jobName + "> Ignore Task : " + TaskKey + "Found In PendingProcessTaskPool");
        } else {

            Long recid = sqlHelperDao.FtechRecID(task.get("analyticsTableName") + "_s");

            Object startDate = new Date();

            if (sqlHelperDao.getConnectionType() > 1)
                startDate = new Timestamp(System.currentTimeMillis());

            String sql2 = "insert into " + task.get("analyticsTableName") + "(RECID ,TASK_ID ,TASK_TYPEID ,TASK_NAME ,START_TIME, EXPECTED_TIME, TYPE_ID, PROCESS_ID )  values (?,?,?,?,?,?,?, ?)";


            List<SaveThreadTransactionInformation> txs = new ArrayList<>();
            ArrayList<Object> pram = new ArrayList<>();
            pram.add(recid);
            pram.add(task.get("recid"));
            pram.add(task.get("tasktypeid"));
            pram.add(task.get("name"));
            pram.add(startDate);
            pram.add(task.get("expected_time"));
            pram.add(task.get("type_id"));
            pram.add(task.get("object_id"));
            SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(sql2,
                    SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);

            // added by Mahmoud for check in
            tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;


            txs.add(tx);
            SaveThread.AddSaveThreadTransaction(txs);


            task.put("analytics_recid", recid);

            NoOfProcessedFoundData++;
            JobUtil.PendingProcessTaskPool.put(TaskKey, task);
        }
    }

    private void RegisterEscalationTask(Map<String, Object> escalationTask) {
        escalationTask.put("tableName", tableName);
        escalationTask.put("processTableName", processTableName);
        escalationTask.put("analyticsTableName", analyticsTableName);
        escalationTask.put("escHistoryTableName", escHistoryTableName);
        escalationTask.put("uda", jobData.get("uda"));
        escalationTask.put("companyName", companyName);
        escalationTask.put("tenantSchema", tenantSchema);
        String TaskKey = JobUtil.CreateEscalationTaskKey(escalationTask);

        if (JobUtil.UnderProcessingEscalationPool.get(TaskKey) != null) {
            NoOfIgnoredRows++;
        } else if (JobUtil.PendingTaskEscalationsPool.get(TaskKey) != null) {
            NoOfIgnoredRows++;
        } else {
            NoOfProcessedFoundData++;
            JobUtil.PendingTaskEscalationsPool.put(TaskKey, escalationTask);
        }
    }

    @SuppressWarnings("unchecked")
    private void CheckSwitchAndProcessProcessingThread() {
        // start process task threads
        if (JobUtil.PendingProcessTaskPool.size() > 0) {
            int AvailableProcessTasks = JobUtil.PendingProcessTaskPool.size();
            int ReadThreads = Integer.valueOf(this.ReadThreads);
            int NThread = (ReadThreads > AvailableProcessTasks) ? AvailableProcessTasks : ReadThreads;

            for (int i = 0; i < NThread; i++) {
                String ThreadName = "PendingProcessTaskThread#" + i;
                if (jobService.isJobRunning(ThreadName) == false) {

                    jobService.scheduleOneTimeJob(ThreadName, JobUtil.getJobClassByName("PendingProcessTask"),
                            new Date(), null);
                }
            }
        }
        // start switch /condition tasks threads
        if (JobUtil.PendingSwitchConditionTasksPool.size() > 0) {
            int AvailableProcessTasks = JobUtil.PendingSwitchConditionTasksPool.size();
            int ReadThreads = Integer.valueOf(this.ReadSwitchTaskThreads);
            int NThread = (ReadThreads > AvailableProcessTasks) ? AvailableProcessTasks : ReadThreads;

            for (int i = 0; i < NThread; i++) {
                String ThreadName = "PendingSwitchAndConditionTasksThread#" + i;
                if (jobService.isJobRunning(ThreadName) == false) {

                    jobService.scheduleOneTimeJob(ThreadName,
                            JobUtil.getJobClassByName("PendingSwitchAndConditionTasks"), new Date(), null);
                }
            }
        }
    }
}
