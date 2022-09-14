package com.ntg.engine.jobs;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.jobs.service.CommonCachingFunction;
import com.ntg.engine.jobs.service.ToDoListRecordBuilder;
import com.ntg.engine.repository.customImpl.SqlHelperDaoImpl;
import com.ntg.engine.service.JobService;
import com.ntg.engine.util.*;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PendingTaskProcessing implements Job {

    @Autowired
    private CommonCachingFunction commonCachingFun;

    @Autowired
    private SqlHelperDaoImpl sqlHelperDao;


    @Autowired
    private JobService jobService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    String jobName;
    
    @Value("${WF.default.vacation.days}")
    private String defaultVacationDays;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        jobName = context.getJobDetail().getKey().toString().split("\\.")[1];
        // JobUtil.Debug("==============Start PendingTaskProcessing ============== <" +
        // jobName + ">");

        Map<String, Object> row = JobUtil.CheckOutPendingTasks();

        if (row == null) {
            // try 10 time before declare it is not data and close
            for (int i = 0; i < 10 && row == null; i++) {
                try {
                    Thread.sleep(1000);
                } catch (Exception c) {
                }
                row = JobUtil.CheckOutPendingTasks();
            }
            if (row == null) {
                JobUtil.Debug("<" + jobName + "> Didn't find Row To Process ... ");
            }
        }
        while (row != null) {
            try {

                String companyName = (Utils.isNotEmpty(row.get("companyName"))) ? row.get("companyName").toString() : "NTG";
                String tenantSchema = (Utils.isNotEmpty(row.get("tenantSchema"))) ? row.get("tenantSchema").toString() : "";

                // plan uda has two tables pm_udaname and pmp_udaname
                Long taskTypeId = JobUtil.convertBigDecimalToLong(row.get("tasktypeid"));

                // if the task come from pm_udaname
                if (row.get("tableType").toString().equals("workPackage")) {
                    // if task type is a work package
                    if (taskTypeId == 1) {
                        ProcessPlanTasks(row, (long) 1, companyName, tenantSchema);
                    } else if (taskTypeId == 2) {
                        // copy plan
                        ProcessPlanTasks(row, (long) 2, companyName, tenantSchema);
                    }
                    // if the task come from pmp_udaname
                } else if (row.get("tableType").toString().equals("process")) {

                    // handle process merge tasks
                    if (taskTypeId == 3) {
                        handleMergeTask(row);
                    } else {
                        // scan sub-process task to copy it's tasks
                        if (taskTypeId == 9) {
                            ProcessPlanTasks(row, (long) 2, companyName, tenantSchema);
                        } else {
                            // work with process tasks flow
                            ProcessProcessTasks(row, companyName, tenantSchema);
                        }
                    }
                }

            } catch (Throwable e) {
                // Error The Task
                if (e instanceof Exception) {
                    NTGMessageOperation.PrintErrorTrace((Exception) e);
                } else {
                    NTGMessageOperation.PrintErrorTrace(new Exception(e));
                }
                List<SaveThreadTransactionInformation> txs = new ArrayList<SaveThreadTransactionInformation>();

                List<Object> pram = new ArrayList<>();
                String Sql = "Update " + row.get("processTableName")
                        + " SET status_id = 20 , ACTUALENDDATE =localtimestamp where RECID = ?";
                pram.add(row.get("recid"));
                SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(Sql,
                        SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);

                // added by Mahmoud for check in
                tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;
                tx.TransactionKey = JobUtil.CreatePendingTaskKey(row);

                txs.add(tx);
                SaveThread.AddSaveThreadTransaction(txs);

            }
            row = JobUtil.CheckOutPendingTasks();
            if (row == null) {
                // try 10 time before declare it is not data and close
                for (int i = 0; i < 10 && row == null; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception c) {
                    }
                    row = JobUtil.CheckOutPendingTasks();
                }
                if (row == null) {
                    JobUtil.Debug("<" + jobName + "> Didn't find more Rows To Process ... ");
                }
            }

        }

        jobService.deleteJob(jobName);
        // JobUtil.Debug("==============End PendingTaskProcessing ============== <" +
        // jobName + ">");

    }

    // handle Process Merge Tasks
    private void handleMergeTask(Map<String, Object> row) {

        String processTableName = row.get("processTableName").toString();
        Long taskRecId = JobUtil.convertBigDecimalToLong(row.get("recid"));
        Long toTaskId = JobUtil.convertBigDecimalToLong(row.get("TASKID"));
        // set operation Id
        String TransactionKey = JobUtil.CreatePendingTaskKey(row);

        List<SaveThreadTransactionInformation> txs = new ArrayList<>();

        // to update merge task status = 4 as finished
        ArrayList<Object> updateFinishmMergeParam = new ArrayList<>();
        String updateFinishMergeTask = "update " + processTableName
                + " set status_id = 4 , actualstartdate = localtimestamp , ACTUALENDDATE = localtimestamp where recid = ?  ";

        updateFinishmMergeParam.add(taskRecId);

        SaveThreadTransactionInformation updateFinishmMergeParamTx = new SaveThreadTransactionInformation(
                updateFinishMergeTask, SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql,
                updateFinishmMergeParam);

        // Setup Transaction Information for Checking After Save
        updateFinishmMergeParamTx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;
        updateFinishmMergeParamTx.TransactionKey = TransactionKey;

        txs.add(updateFinishmMergeParamTx);

        // to activte route to to_task id that come from merge task
        ArrayList<Object> updateRoutemMergeParam = new ArrayList<>();
        String activateRouteOfMergeTask = "update " + processTableName
                + " set status_id = 2 , actualstartdate = localtimestamp "
                + "where taskid in (select TO_TASKID from pm_task_routing  where FROM_TASKID = ?)";

        updateRoutemMergeParam.add(toTaskId);

        SaveThreadTransactionInformation activateRouteOfMergeTaskTX = new SaveThreadTransactionInformation(
                activateRouteOfMergeTask, SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql,
                updateRoutemMergeParam);

        // Setup Transaction Information for Checking After Save
        activateRouteOfMergeTaskTX.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;
        activateRouteOfMergeTaskTX.TransactionKey = TransactionKey;

        txs.add(activateRouteOfMergeTaskTX);

        SaveThread.AddSaveThreadTransaction(txs);

    }

    private void ProcessPlanTasks(Map<String, Object> row, Long type, String companyName, String tenantSchema /*
     * the value 1 is for human tasks which will
     * make the function create ToDoList Record
     */) throws Exception {
        Long TaskTypeID = JobUtil.convertBigDecimalToLong(row.get("tasktypeid"));

        ToDoListRecordBuilder todolistRow;
        List<SaveThreadTransactionInformation> txs = new ArrayList<SaveThreadTransactionInformation>();
        // Wired Actual Start date should be Today not PlannedStartDate
        /// Old Code was row.get("plannedstartdate")
        // Need test date

        row.put("actualstartdate", new Timestamp(System.currentTimeMillis()));

        // set operation Id
        String TransactionKey = JobUtil.CreatePendingTaskKey(row);

        if (type == 1) { /*
         * the value 1 is for human tasks which will make the function create ToDoList
         * Record
         */
            todolistRow = convertTaskToToDoList(row, 1, txs, companyName, tenantSchema);

            todolistRow.setTaskID(null);
            todolistRow.setOpId((long) 20);
            todolistRow.toDo.sendEmailonAssign = true;
            
            ResolvePeriority(todolistRow, companyName);
            
            // passing to save Thread Pool
            SaveThreadTransactionInformation tx1 = new SaveThreadTransactionInformation(todolistRow,
                    SaveThreadTransactionInformation.TxOperaiotns.Add);

            // Settup Transaciton Information for Checking After Save
            tx1.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;
            tx1.TransactionKey = TransactionKey;
            txs.add(tx1);

        }

        List<Object> UpdateParameter = new ArrayList<Object>();

        java.util.Date d = getCurrentDate();
        if (d != null) {
            UpdateParameter.add(d);
        }

        String update = "Update ";
        if (TaskTypeID == 9) {
            update += row.get("processTableName");
            update += " set ACTUALSTARTDATE  = ?,Status_ID = ? where RECID = ?";
            UpdateParameter.add(22);
        } else {
            update += row.get("tableName");
            update += " set ACTUALSTARTDATE  = ? where RECID = ?";
        }

        if (TaskTypeID == 9)
            UpdateParameter.add(Long.parseLong(row.get("recid").toString()));
        else
            UpdateParameter.add(Long.parseLong(row.get("taskid").toString()));

        SaveThreadTransactionInformation tx2 = new SaveThreadTransactionInformation(update,
                SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, UpdateParameter);

        tx2.TransactionKey = TransactionKey;
        tx2.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;
        if (type == 2) {

            List<SaveThreadTransactionInformation> tx3 = copyProcessTasks(row);
            tx3.forEach(tx -> {
                tx.TransactionKey = TransactionKey;
                tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;
                txs.add(tx);
            });
            if (tx3.size() == 0) {
                // no child task convert task to fail
                UpdateParameter.set(1, 17);
            }
        }

        txs.add(tx2);
        SaveThread.AddSaveThreadTransaction(txs);

    }

    private java.util.Date getCurrentDate() {
        // <Yghandor> Wired Code !!!! it seams the devlopoer want to truncate
        // the time
        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        java.sql.Timestamp date = new Timestamp(System.currentTimeMillis());
        String da = dateFormat.format(date);
        Date re = JobUtil.getDateValue(da);
        return re;
    }

    private List<SaveThreadTransactionInformation> copyProcessTasks(Map<String, Object> row) {
        Long TaskTypeID = JobUtil.convertBigDecimalToLong(row.get("tasktypeid"));

        Long processID = null;
        List<Object> UpdateParameter = new ArrayList<Object>();

        List<SaveThreadTransactionInformation> txs = new ArrayList<SaveThreadTransactionInformation>();

        if (TaskTypeID == 9)
            processID = Long.valueOf(row.get("t_sub_planid").toString());
        else
            processID = Long.valueOf(row.get("process_id").toString());

        // select all tasks of process from pm_task table using processID
        // Dev-00000053:fix missing column  for gabr ,
        // restrict the sql of copy task to specific list to avoid adding column issuand
        // reduce copied fields
        String selectSql = "select distinct t.recid,t.tasksequence,t.tasktypeid,\n"
                + "       r.tasktypeid as parent_task_type_ID,\n" + "       t.actualstartdate,\n"
                + "       t.actualenddate,\n" + "       t.assign_millstone,\n" + "       t.prime_type_id,\n"
                + "       t.prime_id,\n" + "       t.assignee_name,\n" + "       t.assignment_info,\n" + "       t.selected_uda_cc,\n"
                + "       t.assignment_uda_id,\n" + "       t.cc_list,\n" + "       t.cc_list_names,\n"
                + "       t.daily_efforts,\n" + "       t.description,\n" + "       t.duration,\n"
                + "       t.emai_template_id,\n" + "       t.expected_time,\n" + "       t.forcastenddate,\n"
                + "       t.forcaststartdate,\n" + "       t.from_mangerial_level,\n" + "       t.minstartdate,\n"
                + "       t.name,\n" + "       t.next_approval_if,\n" + "       t.num_of_assignation,\n"
                + "       t.plannedenddate,\n" + "       t.plannedstartdate,\n"
                + "       t.reset_date_every_reassign,\n" + "       t.scale,\n" + "       t.send_mail_on_assign,\n"
                + "       t.send_mail_on_submit,\n" + "       t.size_of_task,\n" + "       t.stop_condition,\n"
                + "       t.stop_condition_route_to,\n" + "       t.sub_planid,\n" + "       \n"
                + "       t.to_mangerial_level,\n" + "       t.planid,\n" + "       t.sub_status_taskId\n" + "From PM_TASK t \n" + "left join \n"
                + "(Select r.to_taskid,f.tasktypeid from pm_task_routing r \n"
                + " join PM_TASK f on f.recid = r.from_taskid and f.tasktypeid=11) r\n" + " on r.to_taskid = t.recid\n"
                + "where t.planid = ?";

        // get prime id form user started process from pm_

        Long PrimeId = JobUtil.convertBigDecimalToLong(row.get("prime_id"));

        if (PrimeId == null) {
            PrimeId = JobUtil.convertBigDecimalToLong(row.get("created_by_id"));
        }

        List<Map<String, Object>> pm_tasksList = sqlHelperDao.queryForList(selectSql, new Object[]{processID});

        // get last recid of this table
        // long lastSeq =
        // sqlHelperDao.FtechRecID(row.get("processTableName").toString()
        // + "_S");

        Map<String, Object> uda = (Map<String, Object>) row.get("uda");

        for (Map<String, Object> task : pm_tasksList) {
            Long prevTaskTypeID = JobUtil.convertBigDecimalToLong(task.get("parent_task_type_ID"));

            // to remove un existed cols in pmp_table

            task.remove("sub_plan_id");
            task.remove("assignee_name");
            task.remove("cc_list_names");
            task.remove("scale");
            task.remove("xpos");
            task.remove("ypos");
            task.remove("expected_time");
            task.remove("size_of_task");
            task.remove("assignment_uda_id");
            task.remove("employee_group_id");
            task.remove("sub_status_taskId");

            long lastSeq = sqlHelperDao.FtechRecID(row.get("processTableName").toString() + "_S");
            task.put("taskID", task.get("recid"));
            task.put("recid", lastSeq);
            task.put("uda_id", uda.get("recid"));
            if(row.get("priortyid") !=null) {
                task.put("priority", row.get("priortyid"));
            }else{
                task.put("priority", row.get("priority"));
            }
            if (TaskTypeID == 9)
                task.put("object_id", row.get("OBJECT_ID"));
            else
                task.put("object_id", row.get("objectid"));

            task.put("type_id", uda.get("type_id"));
            task.put("ParentPLanTaskID", row.get("recid"));
            task.put("parent_task_id", Long.parseLong(row.get("taskid").toString()));
            task.put("SUBSTATUSID", null);

            long tasktypeid = JobUtil.convertBigDecimalToLong(task.get("tasktypeid"));
            long tasksequence = JobUtil.convertBigDecimalToLong(task.get("tasksequence"));
            if (tasktypeid == 11) {
                // Dev-00000495
                // no need for Start Task as copy the next task as pending direct
                task.put("STATUS_ID", 4); // start task should be pending to start the process
            } else if (tasksequence == 1) {
                task.put("STATUS_ID", 2); // keep back world computability and make task sequance one start by default
            } else if (prevTaskTypeID != null && prevTaskTypeID == 11) {
                // Dev-00000495
                task.put("STATUS_ID", 2); // mean the previous task is start task so make it pending to start immediatly
            } else {
                task.put("STATUS_ID", 1);
            }

            task.remove("parent_task_type_ID");

            if (task.get("reset_date_every_reassign") == "FALSE") {
                task.put("reset_date_every_reassign", BooleanBasedOnConnectionType(0L));
            } else if (task.get("reset_date_every_reassign") == "TRUE") {
                task.put("reset_date_every_reassign", BooleanBasedOnConnectionType(1L));
            }
            if (task.get("send_mail_on_assign") == "FALSE") {
                task.put("send_mail_on_assign", BooleanBasedOnConnectionType(0L));
            } else if (task.get("send_mail_on_assign") == "TRUE") {
                task.put("send_mail_on_assign", BooleanBasedOnConnectionType(1L));
            }
            if (task.get("send_mail_on_submit") == "FALSE") {
                task.put("send_mail_on_submit", BooleanBasedOnConnectionType(0L));
            } else if (task.get("send_mail_on_submit") == "TRUE") {
                task.put("send_mail_on_submit", BooleanBasedOnConnectionType(1L));
            }

            // get asignee user id
            if (Utils.isNotEmpty(task.get("prime_id"))) {
                if (Long.parseLong(task.get("prime_type_id").toString()) == 0) {
                    task.put("prime_id", PrimeId);
                } else if (Long.parseLong(task.get("prime_type_id").toString()) == 1) {
                    EmployeeInfo[] managerList = (PrimeId == null) ? null
                            : commonCachingFun.GetManagerInfo(PrimeId, 1L);
                    if (Utils.isNotEmpty(managerList)) {
                        EmployeeInfo info = managerList[0];
                        task.put("prime_id", info.Emp_ID);
                    }
                }
            }
            ArrayList<Object> prar = CreateParamter(task, (String) row.get("processTableName"), UpdateParameter);

            String insertSQlcopyProcessTasks = createInsertSql(task, (String) row.get("processTableName"), prar);

            SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(insertSQlcopyProcessTasks,
                    SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, prar);
            txs.add(tx);
        }

        return txs;
    }

    private String createInsertSql(Map<String, Object> row, String tableName, ArrayList<Object> prar) {
        // insert the task if not exists only by TaskID && Object_ID
        String sql = "insert into " + tableName + " ( ";

        String values = " Select ";
        int i = 0;
        for (String element : row.keySet()) {
            sql += element;
            values += "?";
            i++;
            if (i < row.keySet().size()) {
                sql += ", ";
                values += ", ";
            } else {
                sql += " )";
                values += " From Dual where Not Exists (Select 1 from " + tableName + " Where TaskID=?"
                        + " And Object_ID = ? and parent_task_id = ? )";
            }
        }
        prar.add(row.get("taskID"));
        prar.add(row.get("object_id"));
        prar.add(row.get("parent_task_id"));
        sql += values;
        return sql;
    }

    private java.util.ArrayList<Object> CreateParamter(Map<String, Object> row, String tableName,
                                                       List<Object> UpdateParameter) {

        java.util.ArrayList<Object> re = new ArrayList<>();
        for (String element : row.keySet()) {
            re.add(row.get(element));
        }
        return re;
    }

    private ToDoListRecordBuilder convertTaskToToDoList(Map<String, Object> map,
                                                        int type /*
     * Type 1 mean work psure you can wakeup me anytime ackedge from plan and 2 is for human tasks from process
     */, List<SaveThreadTransactionInformation> txs, String companyName, String tenantSchema) throws Exception {
//noha
        Long OrginatorID = JobUtil.convertBigDecimalToLong(map.get("created_by_id"));
        String created_by = (String) map.get("created_by");
        String cc = null;

        // created_by_id , o.created_by

        Map<String, Object> uda = (Map<String, Object>) map.get("uda");
        ToDoListRecordBuilder toDo = new ToDoListRecordBuilder();
        
        // added by Mahmoud Atef to set company name for task
        toDo.setCompanyName(companyName);
        toDo.setTenantSchema(tenantSchema);

        String tableName = tenantSchema + uda.get("table_name").toString();
        String udaTable = tenantSchema + uda.get("udaTable").toString();
        String udaTableName = uda.get("udaTable").toString();

        Long moduleId = JobUtil.convertBigDecimalToLong(map.get("objectid"));

        toDo.setModuleId(moduleId);

        // Object --> type_id

        if (uda.get("type_id") instanceof Long)
            toDo.setTypeid((Long) uda.get("type_id"));
        else
            toDo.setTypeid(JobUtil.convertBigDecimalToLong(uda.get("type_id")));

        toDo.setRecId(Long.valueOf(-1));
        // --yghandor why we set done date which the task is just assigned !
        // toDo.setDoneDate((Timestamp) map.get("actualenddate"));

        if (Utils.isNotEmpty(map.get("actualstartdate"))) {
            toDo.setStartDate((Timestamp) map.get("actualstartdate"));
        }
        toDo.setAssignDesc((String) map.get("description"));

        if (type == 1) {
            toDo.setIncom_RecID(JobUtil.convertBigDecimalToLong(map.get("taskid")));
            toDo.setTaskTypeId(0L);
            toDo.setTaskname((String) map.get("taskname"));
        } else if (type == 2) {
            toDo.setIncom_RecID(JobUtil.convertBigDecimalToLong(map.get("recid")));
            toDo.setTaskTypeId(JobUtil.convertBigDecimalToLong(map.get("TASKTYPEID")));
            toDo.setTaskname((String) map.get("name"));
            toDo.setPriority((String) map.get("priority"));

        }

        // yghandor review process ID shouldn't be exists
        // toDo.setProcessID(JobUtil.convertBigDecimalToLong(
        // map.get("processid")));

        toDo.setInheritAllRules(Boolean.parseBoolean(map.get("inherit_All_Rules").toString()));
        toDo.setObject_Id(JobUtil.convertBigDecimalToLong(map.get("object_id")));
        toDo.setUdaID(JobUtil.convertBigDecimalToLong(uda.get("recid")));

        // Aftkasa not needed<yghandor>
        if (Utils.isNotEmpty(map.get("actualstartdate"))) {
            toDo.setFirstStartDate((Timestamp) map.get("actualstartdate"));
        }
        toDo.setAssignDate(new Timestamp(System.currentTimeMillis()));
        Long subStatusTaskId = (Long) map.get("sub_status_taskId");
        if(subStatusTaskId !=null && subStatusTaskId > 0){
            toDo.setSubStatusTaskID(subStatusTaskId);
        } else{
            toDo.setSubStatusTaskID(Long.valueOf(-1));
        }

        String Dquery = "select * from " + udaTable + " a left join " + tableName + " b on b.recid = a."
                + udaTableName + "_id  where b.RecID = " + toDo.toDo.getObject_Id();
        List<Map<String, Object>> Drows2 = jdbcTemplate.queryForList(Dquery);
        toDo.setCcList(null);
        if (map.get("selected_uda_cc") != null) {
            String selectedUdaCC = map.get("selected_uda_cc").toString();
            if (!selectedUdaCC.isEmpty()) {
                if (selectedUdaCC.contains("uda.")) {
                    String result = selectedUdaCC.split("uda.")[1];
                    for (Map<String, Object> obj : Drows2) {
                        Set<String> keys = obj.keySet();
                        for (String key : keys) {
                            if (key.equalsIgnoreCase(result) && obj.get(key) != null) {
                                cc = obj.get(key).toString();
                            }
                        }
                    }
                } else {
                    cc = selectedUdaCC;
                }
            }
        }
        if (type == 1) { // task from pm_udaname
            toDo.setUdaName(uda.get("uda_table_name").toString());
            toDo.setTaskID(JobUtil.convertBigDecimalToLong(map.get("taskid")));
            if (Utils.isNotEmpty(map.get("priortyid"))) {
                toDo.setPriority(map.get("priortyid").toString());
            }
            Long primeType = JobUtil.convertBigDecimalToLong(map.get("primeTypeId"));
            Long taskPrimeId = JobUtil.convertBigDecimalToLong(map.get("primeId"));

            if (primeType == 0) {
                toDo.setTo_EmployeeID(OrginatorID);
                toDo.setToDepName(created_by);
            } else if (primeType == 1) {
                EmployeeInfo[] managerList = commonCachingFun.GetManagerInfo(OrginatorID, 1L);
                if (Utils.isNotEmpty(managerList)) {
                    EmployeeInfo info = managerList[0];
                    toDo.setTo_EmployeeID(info.Emp_ID);
                    toDo.setToDepName(info.Name);
                } else {
                    toDo.setTo_EmployeeID(OrginatorID);
                    toDo.setToDepName(created_by);
                }

            } else if (primeType == 2) {
                // PrimeType 2 which mean it is assign to group and group id in primeId
                toDo.setTo_GroupID(taskPrimeId);
            } else if (primeType == 3) {
                // PrimeType 3 mean assign to specific emplyee and the employee id in primeId
                Long empId = taskPrimeId;
                toDo.setTo_EmployeeID(empId);

            } else if (primeType == 4) { // assignment rule
                executeAssignmentRule(map, toDo, OrginatorID, created_by, 1L, txs, companyName);
            }

            if (map.get("assigneename") != null) {
                // it is depend on front resolve this value
                toDo.setToDepName(map.get("assigneename").toString());
            }

            if (Utils.isNotEmpty(map.get("cclist")) && Utils.isNotEmpty(map.get("cclistid"))) {
                toDo.setCcList(map.get("cclistid").toString());
            } else
                toDo.setCcList(null);
            // was missing required completion date for work packedge //need test
           // toDo.setReqCompletionDate((Timestamp) map.get("plannedEndDate"));
            
            if (map.get("actualstartdate") != null) {
            	Long taskDuration = Long.parseLong(map.get("duration").toString());
            	Date taskStartDate = (Date) map.get("actualstartdate");
                Date newDate = Utils.calculateDateBasedOnWorkingDays(taskStartDate, taskDuration.intValue(), defaultVacationDays);
                toDo.setReqCompletionDate(new Timestamp(newDate.getTime()));
            }

        } else if (type == 2) { // task from pmp_udaNmae
            Long fromManagerLevel = JobUtil.convertBigDecimalToLong(map.get("from_mangerial_level"));
            Long toManagerLevel = JobUtil.convertBigDecimalToLong(map.get("to_mangerial_level"));

            toDo.setFromMangerialLevel(fromManagerLevel);
            toDo.setToMangerialLevel(toManagerLevel);

            if (map.get("next_approval_if") != null)
                toDo.setNextApprovalIf(map.get("next_approval_if").toString());

            if (map.get("taskid") != null)
                toDo.setTaskID(Long.parseLong(map.get("taskid").toString()));

            // didn't check pricess falg if need restart date Always time or not
            boolean resetAssignDate;
            if (sqlHelperDao.getConnectionType() > 1)
                resetAssignDate = (Boolean) map.get("resetAssignDate");
            else
                resetAssignDate = JobUtil.getBooleanValue(map.get("resetAssignDate"));

            if (map.get("actualstartdate") != null && resetAssignDate == false) {
                toDo.setAssignDate((Timestamp) map.get("actualstartdate"));
            } else {
                toDo.setAssignDate(new Timestamp(System.currentTimeMillis()));
            }

            if (map.get("actualstartdate") != null) {
            	Long taskDuration = Long.parseLong(map.get("duration").toString());
            	Date taskStartDate = (Date) map.get("actualstartdate");
                Date newDate = Utils.calculateDateBasedOnWorkingDays(taskStartDate, taskDuration.intValue(), defaultVacationDays);
                toDo.setReqCompletionDate(new Timestamp(newDate.getTime()));
            }

            boolean is_expiry_task = JobUtil.getBooleanValue(map.get("is_expiry_task"));

            if (is_expiry_task) {
                long expiration_days = JobUtil.convertBigDecimalToLong(map.get("expiration_days"));
                if (expiration_days > 0) {
                	Date newDate = Utils.calculateDateBasedOnWorkingDays(new Date(), (int) expiration_days, defaultVacationDays);
                    toDo.setExpirationDate(new Timestamp(newDate.getTime()));
                }
            }


            toDo.setPMtaskRecid(JobUtil.convertBigDecimalToLong(map.get("ParentPLanTaskID")));
            toDo.setParentTaskId(Long.parseLong(map.get("PARENT_TASK_ID").toString()));
            String AdditionalUdaTableNames = (String) uda.get("additional_uda_table_names");
            toDo.setUdaName(AdditionalUdaTableNames
                    .split(DatabaseTableNameUtils.additionalTablesSeparator)[DatabaseTableNameUtils.processTableIndex]);
            // if() to be added here question task

            // task type 4 is Question Tasks
            if (JobUtil.convertBigDecimalToLong(map.get("tasktypeid")) == 4) {
                long val = JobUtil.convertBigDecimalToLong(map.get("recid"));
                // delete (-) negative sign By Mahmoud
                toDo.setIncom_RecID(val);
            } else if (JobUtil.convertBigDecimalToLong(map.get("tasktypeid")) == 1) {
                // normal human task
                toDo.setIncom_RecID(JobUtil.convertBigDecimalToLong(map.get("recid")));
            }

            Long processPrimeTypeId = JobUtil.convertBigDecimalToLong(map.get("prime_type_id"));
            Long processPrimeId = JobUtil.convertBigDecimalToLong(map.get("prime_id"));
            // Dev-00000626 : fix reassign
            // Dev-00000686 :
            // when reassign from front will put in assignment_uda_id -10 to ignore the uda
            // values
            Object asignUDA = map.get("tassignment_uda_id");
            Object UserSecltionAssignUDA = map.get("assignment_uda_id");
            if (asignUDA != null && (UserSecltionAssignUDA == null
                    || JobUtil.convertBigDecimalToLong(UserSecltionAssignUDA) != -10)) {
                long asignUDAId = Long.parseLong(asignUDA.toString());
                long objectId = Long.parseLong(map.get("object_id").toString());
                String sql = "select ai_Group_Id_" + asignUDAId + " , ai_Group_" + asignUDAId + ", ai_mem_uid_" + asignUDAId + " from " + udaTable
                        + " where " + udaTableName + "_id = " + objectId;
                List<Map<String, Object>> savedObject = jdbcTemplate.queryForList(sql);
                Long useId = JobUtil.convertBigDecimalToLong(savedObject.get(0).get("ai_mem_uid_" + asignUDAId));
                Long groupId = JobUtil.convertBigDecimalToLong(savedObject.get(0).get("ai_Group_Id_" + asignUDAId));
                Object groupName = (savedObject.get(0).get("ai_Group_" + asignUDAId));

                if (useId != null && useId > 0) {
                    toDo.setTo_EmployeeID(useId);
                    processPrimeTypeId = null; // to avoid next processing for assignation
                } else if (groupId != null && groupId > 0) {
                    toDo.setTo_GroupID(groupId);
                    toDo.setToDepName((groupName == null) ? null : groupName.toString());
                    processPrimeTypeId = null; // to avoid next processing for assignation
                }
            }

            if (Utils.isNotEmpty(processPrimeTypeId)) {
                if (processPrimeTypeId == 0) {
                    // PrimeType 0 which mean it is assign to originator
                    toDo.setTo_EmployeeID(OrginatorID);
                    toDo.setToDepName(created_by);

                } else if (processPrimeTypeId == 1) {
                    // PrimeType 1 which mean it is assign to manager
                    EmployeeInfo[] managerList = commonCachingFun.GetManagerInfo(OrginatorID, fromManagerLevel);
                    if (Utils.isNotEmpty(managerList)) {
                        EmployeeInfo info = managerList[0];
                        toDo.setTo_EmployeeID(info.Emp_ID);
                        toDo.setToDepName(info.Name);
                    } else {
                        toDo.setTo_EmployeeID(OrginatorID);
                        toDo.setToDepName(created_by);
                    }
                }
                if (processPrimeTypeId == 2) {// PrimeType 2 which mean it is assign to group
                    toDo.setTo_GroupID(processPrimeId);
                } else if (processPrimeTypeId == 3) {
                    // PrimeType 3 which mean it is assign to employee
                    toDo.setTo_EmployeeID(processPrimeId);
                } else if (processPrimeTypeId == 4) {
                    // assignment rule
                    executeAssignmentRule(map, toDo, OrginatorID, created_by, fromManagerLevel, txs, companyName);
                }
                if ((map.get("assignment_info") != null
                        && !map.get("assignment_info").toString().isEmpty())||processPrimeTypeId == 5) {
                    if (map.get("assignment_info").toString().contains("uda.")) {
                        String result = map.get("assignment_info").toString().split("uda.")[1];
                        for (Map<String, Object> obj : Drows2) {
                            Set<String> keys = obj.keySet();
                            for (String key : keys) {
                                if (key.equalsIgnoreCase(result) && obj.get(key) != null) {
                                    if (processPrimeTypeId == 5)
                                        toDo.setDynamic_mail(obj.get(key).toString());
                                    else
                                        toDo.setActionableEmail(obj.get(key).toString());
                                }
                            }
                        }
                    } else {
                        if (processPrimeTypeId == 5)
                            toDo.setDynamic_mail(map.get("assignment_info").toString());
                        else
                            toDo.setActionableEmail(map.get("assignment_info").toString());
                    }

                }
            }
        }


        if (Utils.isNotEmpty(map.get("cc_list"))) {
            if (cc == null) {
                toDo.setCcList(map.get("cc_list").toString());
            } else {
                toDo.setCcList(map.get("cc_list").toString() + "," + cc);
            }
        } else
            toDo.setCcList(cc);
        // resolve Employee IMage & Name & Email if needed
        long empId = toDo.toDo.getTo_EmployeeID();
        if (empId > 0) {
            EmployeeFullInfoResponse empInfo = commonCachingFun.getEmployeeFullInfo(empId);
            toDo.setorderedMembersImagesIds( empId + ",");
            toDo.setDynamic_mail(empInfo.returnValue.Email);
            if (empInfo != null && empInfo.returnValue != null) {
                toDo.setToDepName(empInfo.returnValue.Name);

            }
        }

        // =============added to push data in table emp_notfication to push
        // notifications =============
        addPushNotificationToTable(toDo, txs);

        // founds that if assign_description exceed 255 characters
        // Dev-00000466 <yghandor>
        if (toDo.toDo.assignDesc != null && toDo.toDo.assignDesc.length() > 254) {
            // Limi Assign Description to avoid exceptions
            toDo.setAssignDesc( toDo.toDo.assignDesc.substring(0, 250) + "...");
        }


        return toDo;
    }

    //@addedBy:Aya.Ramadan=> Dev-00002285: Push notification to group members
    private List<SaveThreadTransactionInformation> addPushNotificationToTable(ToDoListRecordBuilder toDo, List<SaveThreadTransactionInformation> txs) {
        String pushInNotificationTable = "insert into emp_notfication(company_name, DESCRIPTION , TASK_ID , NOTIFICATION_TIME,GENERIC_OBJECT_ID,task_table_name,RECID,TO_USER_ID) Values(?,?,?,?,?,?,?,?)";
        ArrayList<Object> params = new ArrayList<>();
        params.add(toDo.toDo.getCompanyName());
        String desc = toDo.toDo.assignDesc;
        if (desc != null) {
            if (desc.length() > 255) {
                desc = desc.substring(0, 252) + "..";
            }
            params.add(desc);
        } else {
            params.add(toDo.toDo.getTaskname());
        }
        //@AddedBy:Aya.Ramadan => Dev-00001984:add task recid  in notification object
        params.add(toDo.toDo.getIncom_RecID());
        params.add(new Date());
        params.add(toDo.toDo.getObject_Id());
        params.add(toDo.toDo.getUdaName());

        if (toDo != null && toDo.toDo.to_EmployeeID != null && toDo.toDo.to_EmployeeID > 0) { // Notification for Emp Only ,
            long recid = this.sqlHelperDao.FtechRecID("emp_notfication_s");
            ArrayList<Object> params1 = new ArrayList<>();
            params1.addAll(params);
            params1.add(recid);
            params1.add(toDo.toDo.to_EmployeeID);
            SaveThreadTransactionInformation PushNotficationTRansaction = new SaveThreadTransactionInformation(
                    pushInNotificationTable, SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, params1);

            txs.add(PushNotficationTRansaction);
        }
        if (toDo != null && Utils.isNotEmpty(toDo.toDo.getTo_GroupID())) { // Notification for Emp Only ,
            Employee[] employees = commonCachingFun.getAllGroupEmployees(toDo.toDo.getTo_GroupID());
            if (employees != null) {
                for (Employee emp : employees) {
                    ArrayList<Object> params1 = new ArrayList<>();
                    params1.addAll(params);
                    long recid = this.sqlHelperDao.FtechRecID("emp_notfication_s");
                    params1.add(recid);
                    params1.add(emp.UserID);
                    SaveThreadTransactionInformation PushNotficationTRansaction = new SaveThreadTransactionInformation(
                            pushInNotificationTable, SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, params1);

                    txs.add(PushNotficationTRansaction);
                }
            }
        }
        return txs;
    }

    private void ProcessProcessTasks(Map<String, Object> row, String companyName, String tenantSchema) throws Exception {

        // added to resolve description
//noha
        Long TaskTypeID = JobUtil.convertBigDecimalToLong(row.get("tasktypeid"));

        String resolvedDescription = null;
        ArrayList<Object> params = new ArrayList<>();
        String description = (row.get("tdescription") != null) ? row.get("tdescription").toString() : null;
        Map ObjectOfProcess = null;
        if (Utils.isNotEmpty(description) && description.contains("{{")) {
            ObjectOfProcess = commonCachingFun.loadCrmObject(row, null, null, companyName);
            resolvedDescription = commonCachingFun.resolveDescription(row, description, null, null, ObjectOfProcess, false, companyName, tenantSchema);
            row.put("description", resolvedDescription);
        }

        String durationuda = (row.get("durationuda") != null) ? row.get("durationuda").toString() : null;

        if (Utils.isNotEmpty(durationuda)) {
            if (!durationuda.contains("{{")) {
                durationuda = "{{" + durationuda + "}}";
            }

            String resolveddurationuda = commonCachingFun.resolveDescription(row, durationuda, null, null, ObjectOfProcess, false, companyName, tenantSchema);

            try {
                String noSpaceStr = resolveddurationuda.replaceAll("\\s", "");
                Long result = Long.parseLong(noSpaceStr);
                //Override the Duration Values with dynamic value
                if (result > 0) {
                    row.put("duration", result);
                }
            } catch (NumberFormatException ex) { // handle your exception
                System.out.println("Error Resolving duration --> ");
                NTGMessageOperation.PrintErrorTrace(ex);
            }

        }

        String priorityUDA = (row.get("priorityUDA") != null) ? row.get("priorityUDA").toString() : null;
        if (Utils.isNotEmpty(priorityUDA)) {
            if (!priorityUDA.contains("{{")) {
                priorityUDA = "{{" + priorityUDA + "}}";
            }
            String resolvedpriorityUDA = commonCachingFun.resolveDescription(row, priorityUDA, null, null, ObjectOfProcess, false, companyName, tenantSchema);
            if (resolvedpriorityUDA != null && !resolvedpriorityUDA.equals("")) {
                row.put("priority", resolvedpriorityUDA);
            }
        }


        List<Object> pram = new ArrayList<>();

        SaveThreadTransactionInformation tx2;

        String sql2;

        List<SaveThreadTransactionInformation> txs = new ArrayList<>();

        Long numberOfRecall = (row.get("NUM_OF_RECALL") == null) ? null
                : Long.parseLong(row.get("NUM_OF_RECALL").toString());

        Long stopCondition = (row.get("STOP_CONDITION") == null) ? null
                : Long.parseLong(row.get("STOP_CONDITION").toString());
        Boolean stopConditionFlag = false;

        String Sql = "Update " + row.get("processTableName") + " SET ";
        if (numberOfRecall != null && stopCondition != null && numberOfRecall > 0
                && numberOfRecall > stopCondition - 1) {
            // added to check stop condition
            List<Object> pram2 = new ArrayList<>();
            String interruotRoute = "Update " + row.get("processTableName") + " set status_id= 2 where TASKID = ? ";
            pram2.add(row.get("STOP_CONDITION_ROUTE_TO"));

            SaveThreadTransactionInformation tx3 = new SaveThreadTransactionInformation(interruotRoute,
                    SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram2);
            txs.add(tx3);

            Sql += " status_id= 21,"; //20 IS EXCEPTION AND STOP SHOULD BE BREACKED TO OTHER TASKS
            //21 IS Process No Next Route
            stopConditionFlag = true;

        } else if (TaskTypeID == 10/* Email Task */) {
            Sql += " status_id= 8,";
        } else if (TaskTypeID == 7/* Switch Task */
                || TaskTypeID == 8/* Process Task */
                || TaskTypeID == 2 /* Conditional Task */
        ) {

            Sql += " status_id= 4,";//CODE WILL NEVER HAPPENED AS HTIS TYPES GO TO OTHER HANDLER

        } else if (TaskTypeID == 5 // dumy task
                || TaskTypeID == 6 // finish task
                || TaskTypeID == 9 // sub process task
                || TaskTypeID == 11 // Start task

        ) {

            Sql += " status_id= 4,";
        } else {
            Sql += " status_id= 3,";
        }
        if (numberOfRecall != null) {
            if (numberOfRecall == 0) {
                Sql += " NUM_OF_RECALL= 1,";
            } else {
                if (stopCondition != null) {
                    if (numberOfRecall < stopCondition + 1) {
                        Sql += "  NUM_OF_RECALL= " + (numberOfRecall + 1) + ",";
                    }
                }
            }
        }

        Sql += " ACTUALSTARTDATE =localtimestamp ";
        if (resolvedDescription != null)
            Sql += ", DESCRIPTION = '" + resolvedDescription + "'";

        if ((TaskTypeID == 5 || TaskTypeID == 11) && !stopConditionFlag) {
            Sql += ",ACTUALENDDATE =localtimestamp ";
            String processTableName = (String) row.get("processTableName");
            Long recid = JobUtil.convertBigDecimalToLong(row.get("recid"));
            Long TaskID = JobUtil.convertBigDecimalToLong(row.get("taskId"));
            RouteToNext(processTableName, recid, TaskID, txs);
        } else if (TaskTypeID == 6) {

            Long ParentPlanTaskID = JobUtil.convertBigDecimalToLong(row.get("ParentPlanTaskID"));
            if (ParentPlanTaskID != null && ParentPlanTaskID > 0) {
                // the parent is a task need to be update and route to next
                // update parent task to status 14 which child tasks completed
                String processTableName = (String) row.get("processTableName");
                ArrayList<Object> UpdateFinishparams = new ArrayList<>();

                sql2 = "update  " + processTableName + " set STATUS_ID = ? , ACTUALEndDATE = ? where recid = ?";

                UpdateFinishparams.add(14);
                UpdateFinishparams.add(new Date());
                UpdateFinishparams.add(ParentPlanTaskID); // update parent task in the parent plan to fnish

                txs.add(new SaveThreadTransactionInformation(sql2,
                        SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, UpdateFinishparams));
                // then route to next
                // Route From Parent Tasks
                Long PARENT_TASK_ID = JobUtil.convertBigDecimalToLong(row.get("PARENT_TASK_ID"));
                RouteToNext(processTableName, ParentPlanTaskID, PARENT_TASK_ID, txs);

            } else {
                // the parent is work packedge

                Sql += ",ACTUALENDDATE =localtimestamp ";

                sql2 = "update " + row.get("tableName") + " set ACTUALENDDATE = localtimestamp  where RECID= ?";
                params.add(Long.parseLong(row.get("parent_task_id").toString()));

                tx2 = new SaveThreadTransactionInformation(sql2,
                        SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, params);
                txs.add(tx2);
            }
        }

        row.put("actualstartdate", new Timestamp(System.currentTimeMillis()));

        if (TaskTypeID != 5 && TaskTypeID != 6 && TaskTypeID != 8 && TaskTypeID != 7 && TaskTypeID != 2
                && TaskTypeID != 9 && TaskTypeID != 11) {

            SaveThreadTransactionInformation tx1 = null;
            // added By Mahmoud to handle stop condition
            if (!stopConditionFlag) {
                ToDoListRecordBuilder todolistRow = convertTaskToToDoList(row, 2, txs, companyName, tenantSchema);

                ResolvePeriority(todolistRow, companyName);

                Sql += ",reqcomplationdate =?";
                pram.add(todolistRow.toDo.getReqCompletionDate());
                // <yghandor> need to test oracle & postgress as well

                if (sqlHelperDao.getConnectionType() > 1)
                    todolistRow.toDo.sendEmailonAssign = row.get("SEND_MAIL_ON_ASSIGN") == null ? false
                            : (Boolean) row.get("SEND_MAIL_ON_ASSIGN");
                else
                    todolistRow.toDo.sendEmailonAssign = (row.get("SEND_MAIL_ON_ASSIGN") == null
                            || Long.valueOf(row.get("SEND_MAIL_ON_ASSIGN").toString()) == 0) ? false : true;

                // added to handle email template on email task
                if (row.get("emai_template_id") != null) {
                    Long emailId = JobUtil.convertBigDecimalToLong(row.get("emai_template_id"));
                    todolistRow.setEmailTemplateId(emailId);
                    //already save thread will add it to the pool after saving the task
                    if (TaskTypeID == 10) {
                        JobUtil.AddMailTaskToPool(todolistRow.toDo);
                    }

                }
                todolistRow.setOpId((long) 7);

                // passing to save Thread Pool if
                if (TaskTypeID != 10) {
                    tx1 = new SaveThreadTransactionInformation(todolistRow,
                            SaveThreadTransactionInformation.TxOperaiotns.Add);
                }
            }
            // Setup Transaction Information for Checking After Save
            if (tx1 != null) {
                tx1.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;
                tx1.TransactionKey = JobUtil.CreatePendingTaskKey(row);
                txs.add(tx1);
            }
        }

        Sql += " Where RECID = ?";

        pram.add(row.get("recid"));

        SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(Sql,
                SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);
//        if (TaskTypeID == 5)//Dev-00003053 : Some Time Work Flow Have Some Pending Records (under processing) not removed
        tx.TransactionKey = JobUtil.CreatePendingTaskKey(row);
        tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskHandler;
        txs.add(tx);

        SaveThread.AddSaveThreadTransaction(txs);
    }

    private void ResolvePeriority(ToDoListRecordBuilder todolistRow, String companyName) {

        String periority = todolistRow.toDo.getPriority();
        if (periority != null && periority.equals("") == false) {
            String noSpaceurguncy = periority.replaceAll("\\s", "").toLowerCase();
            List<Map<String, Object>> urgancy = commonCachingFun.geturgancyList(companyName);

            //find by name
            for (Map<String, Object> map : urgancy) {
                if (map.get("name") != null) {
                    String value = map.get("name").toString().toLowerCase();
                    String noSpacevalue = value.replaceAll("\\s", "");
                    if (noSpacevalue.equals(noSpaceurguncy)) {

                        String recId = map.get("recId").toString();
                        todolistRow.seturgent_ID( JobUtil.convertBigDecimalToLong(recId));
                        // get ID & color & set
                        todolistRow.setPriority((String) map.get("name"));
                        todolistRow.setPriorityColor((String) map.get("color"));
                        return;

                    }
                }
            }

            //find by ID

            for (Map<String, Object> map : urgancy) {
                if (map.get("recId") != null) {
                    String value = map.get("recId").toString().toLowerCase();
                    String noSpacevalue = value.replaceAll("\\s", "");
                    if (noSpacevalue.equals(noSpaceurguncy)) {

                        String recId = map.get("recId").toString();
                        todolistRow.seturgent_ID (JobUtil.convertBigDecimalToLong(recId));
                        // get ID & color & set
                        todolistRow.setPriority((String) map.get("name"));
                        todolistRow.setPriorityColor((String) map.get("color"));
                        return;

                    }
                }
            }


        }


    }

    private void RouteToNext(String processTableName, Long FromProcessTaskRecID, Long TaskID,
                             List<SaveThreadTransactionInformation> txs) {
        String sql2;
        // route to next task for dumat & start & sub process taskss
        if (this.sqlHelperDao.getConnectionType() == 1) {
            // r.STATUS_ID != 20 and Wired condiation removed as no status in routing except
            // for merge tasks<yghandor>
            sql2 = "Select t.RecID,"

                    + "sysdate + ((SELECT lag_time from PM_TASK_ROUTING r where(r.to_taskid = t.TASKID  "
                    + " AND r.from_taskid= ?" + ")) / 24 /60) mDate From " + processTableName
                    + " t where EXISTS ( SELECT c.RecID , d.lag_time ,c.name  FROM	" + processTableName
                    + " B, PM_TASK_ROUTING D, " + processTableName
                    + " C  WHERE (d.to_taskid = C.TASKID) AND (B.TASKID = d.from_taskid )  "
                    + " AND ( C.status_id not in  (2,3) ) AND b.recid = ? and b.Object_ID =  c.Object_ID "
                    + " AND t.recid = c.recid ";
        } else {// postgres
            // r.STATUS_ID != 20 and Wired condiation removed as no status in routing except
            // for merge tasks<yghandor>
            sql2 = "Select t.RecID,"

                    + " localtimestamp + INTERVAL '1 day' * ((SELECT lag_time from PM_TASK_ROUTING r where(r.to_taskid = t.TASKID  "
                    + " AND r.from_taskid= ?" + ")) / 24 /60) mDate From " + processTableName
                    + " t where EXISTS ( SELECT c.RecID , d.lag_time ,c.name  FROM	" + processTableName
                    + " B, PM_TASK_ROUTING D, " + processTableName
                    + " C  WHERE (d.to_taskid = C.TASKID) AND (B.TASKID = d.from_taskid )  "
                    + " AND ( C.status_id not in  (2,3) ) AND b.recid = ? and b.Object_ID =  c.Object_ID "
                    + " AND t.recid = c.recid ";
        }
        ArrayList<Object> params = new ArrayList<>();
        params.add(TaskID);
        params.add(FromProcessTaskRecID);

        sql2 += " And C.PARENT_TASK_ID = b.PARENT_TASK_ID )";

        List<Map<String, Object>> TargetTasks = sqlHelperDao.queryForList(sql2, params.toArray());
        if (Utils.isNotEmpty(TargetTasks)) {
            for (Map<String, Object> task : TargetTasks) {
                long TTaskRecid = JobUtil.convertBigDecimalToLong(task.get("recid"));
                Object mDate = task.get("mDate");
                if (mDate == null) {
                    mDate = new Date();
                }
                // Build Sqls to route to traget tasks

                sql2 = "update  " + processTableName
                        + " set STATUS_ID = 2 , ACTUALSTARTDATE = ?, MINSTARTDATE= ? where recid = ?";
                params = new ArrayList<>();
                params.add(new Timestamp(System.currentTimeMillis()));
                params.add(mDate);
                params.add(TTaskRecid);
                SaveThreadTransactionInformation tx2 = new SaveThreadTransactionInformation(sql2,
                        SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, params);
                txs.add(tx2);

            }
        }

    }

    /**
     * @param row
     * @param toDo
     * @author Mahmoud Atef -- Handling task assignation using rules
     */
    private void executeAssignmentRule(Map<String, Object> row, ToDoListRecordBuilder toDo, Long OrginatorID, String created_by,
                                       Long managerLevel, List<SaveThreadTransactionInformation> txs, String companyName) throws Exception {

        Map<String, Object> uda = (Map<String, Object>) row.get("uda");
        Long typeId = JobUtil.convertBigDecimalToLong(uda.get("type_id"));
        String assignmentRuleTrace = null;
        Long rowPrimeTypeId = null, rowPrimeId = null, taskRecId = null, object_id = null;
        String updateTable = null;

        if (Utils.isNotEmpty(row)) {

            String TableType = row.get("tableType").toString();

            if (TableType.equals("workPackage")) {// workPackage
                rowPrimeTypeId = JobUtil.convertBigDecimalToLong(row.get("primeTypeId"));
                rowPrimeId = JobUtil.convertBigDecimalToLong(row.get("primeId"));
                taskRecId = JobUtil.convertBigDecimalToLong(row.get("taskId"));
                object_id = JobUtil.convertBigDecimalToLong(row.get("objectid"));
                updateTable = row.get("tableName").toString();
            } else {// process
                rowPrimeTypeId = JobUtil.convertBigDecimalToLong(row.get("prime_type_id"));
                rowPrimeId = JobUtil.convertBigDecimalToLong(row.get("prime_id"));
                taskRecId = JobUtil.convertBigDecimalToLong(row.get("recId"));
                object_id = JobUtil.convertBigDecimalToLong(row.get("object_id"));
                updateTable = row.get("processTableName").toString();
            }

            if (rowPrimeTypeId == 4 && Utils.isNotEmpty(rowPrimeId)) {

                // call crm to get assignee id
                Map<String, Object> assign = commonCachingFun.checkAssignmentRule(rowPrimeId, typeId, object_id,
                        created_by, OrginatorID, false, companyName);

                // get mapped data
                Long toEmpId = null;
                Long toGroupId = null;
                if (Utils.isNotEmpty(assign.get("toEmployeeId"))) {
                    toEmpId = Long.parseLong(assign.get("toEmployeeId").toString());
                }
                
                else if (Utils.isNotEmpty(assign.get("toGroupId"))) {
                    toGroupId = Long.parseLong(assign.get("toGroupId").toString());
                }
                assignmentRuleTrace = (assign.get("assignmentRuleTrace") == null) ? null
                        : assign.get("assignmentRuleTrace").toString();
                String toDepName = (assign.get("toDepName") == null) ? null : assign.get("toDepName").toString();

                // assign to employee if assigned to employee
                if (Utils.isNotEmpty(toEmpId)) {
                    toDo.setTo_EmployeeID(toEmpId);
                }
                // assign to group if assigned to group
                if (Utils.isNotEmpty(toGroupId)) {
                    toDo.setTo_GroupID(toGroupId);
                }
                // set assignee name
                if (Utils.isNotEmpty(toDepName)) {
                    toDo.setToDepName(toDepName);
                }

            }
        }

        // need check if no rule valid then to assign to originator to
        // avoid task lost
        if (assignmentRuleTrace == null) {
            assignmentRuleTrace = "No Assignment Rule Are Valid , assign to Originator ";
            toDo.setTo_EmployeeID(OrginatorID);
            toDo.setToDepName("Originator");
        }

        if (updateTable != null && taskRecId != null) {
            String sql = "UPDATE " + updateTable + " SET ASSIGNMENT_RULE_TRACE = ? WHERE RECID = ?";

            ArrayList<Object> PaframeterList = new ArrayList<>();
            PaframeterList.add(assignmentRuleTrace);
            PaframeterList.add(taskRecId);

            SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(sql,
                    SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, PaframeterList);
            txs.add(tx);
        }

    }

    private Object BooleanBasedOnConnectionType(Long value) {

        if (sqlHelperDao.getConnectionType() == 1) {

            if (value != null) {

                if (value.equals(1L))
                    return 1L;
                else
                    return 0L;
            }
        } else {

            if (value != null) {

                if (value.equals(1L))
                    return true;
                else
                    return false;
            }
        }

        return value;
    }

}
