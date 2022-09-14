package com.ntg.engine.jobs;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.jobs.service.CommonCachingFunction;
import com.ntg.engine.service.JobService;
import com.ntg.engine.util.UpdateRouteToTask;
import com.ntg.engine.util.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * @author abdelrahman Updated By Mahmoud Atef
 */
public class PendingSwitchAndConditionTasks implements Job {

    @Autowired
    private CommonCachingFunction commonCachingFun;

    @Autowired
    private JobService jobService;

    @Autowired
    private RestTemplate restTemplate;

    String jobName;

    // for service to route tasks
    @Value("${wf_routeTasks}")
    String wf_routeTasks;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        jobName = context.getJobDetail().getKey().toString().split("\\.")[1];
        JobUtil.Debug("==============Start PendingSwitchAndConditionTasks ============== <" + jobName + ">");
        try {
            Map<String, Object> row = JobUtil.CheckOutSwitchAndConditionTasks();

            while (row != null) {
                String companyName = (Utils.isNotEmpty(row.get("companyName"))) ? row.get("companyName").toString() : "NTG";
                processSwitchConditionTasks(row, companyName);

                row = JobUtil.CheckOutSwitchAndConditionTasks();
            }
        } catch (Exception e) {
            NTGMessageOperation.PrintErrorTrace(e);

        }
        jobService.deleteJob(jobName);
        //JobUtil.Debug("==============End PendingSwitchAndConditionTasks ============== <" + jobName + ">");

    }

    /**
     * @param typeId
     * @param taskId
     * @param row
     * @throws Exception
     */
    public void processSwitchConditionTasks(Map<String, Object> row, String companyName) throws Exception {

        try {

            Long typeId = Long.parseLong(row.get("TYPE_ID").toString());
            Long taskId = Long.parseLong(row.get("TASKID").toString());

            List<SaveThreadTransactionInformation> txs = new ArrayList<SaveThreadTransactionInformation>();

            // set operation Id
            String TransactionKey = JobUtil.CreateSwitchConditionTasksKey(row);

            HttpHeaders headersRoute = new HttpHeaders();
            headersRoute.setContentType(MediaType.APPLICATION_JSON);
            headersRoute.set("SessionToken", commonCachingFun.BackEndLogin(companyName));
            headersRoute.set("User-Agent", "Smart2GoWorkFlowEngine");

            List<Map<String, Object>> listToBeSent = new ArrayList<Map<String, Object>>();
            Map<String, Object> mapToBeSent = new Hashtable<String, Object>();
            mapToBeSent.put("recid", Long.parseLong(row.get("OBJECT_ID").toString()));
            mapToBeSent.put("taskId", taskId);
            mapToBeSent.put("tableName", row.get("processTableName"));
            mapToBeSent.put("tasktypeid", row.get("tasktypeid"));
            mapToBeSent.put("processTableRowRecId", row.get("recid"));
            mapToBeSent.put("parentTaskId", row.get("parent_task_id"));

            // check here to not make null pointer in case of not parent plan exists
            if (row.get("parentplantaskid") != null)
                mapToBeSent.put("parentPlanTaskId", row.get("parentplantaskid"));

            listToBeSent.add(mapToBeSent);

            String routeTasks = wf_routeTasks + "/" + taskId + "/" + typeId;

            for (Map<String, Object> eachList : listToBeSent) {
                HttpEntity<Map<String, Object>> entityRule = new HttpEntity<>(eachList, headersRoute);
                ResponseEntity<UpdateRouteToTask> responseEntity = restTemplate.exchange(routeTasks, HttpMethod.POST,
                        entityRule, UpdateRouteToTask.class);
                UpdateRouteToTask updaterouteTotask = responseEntity.getBody();

                String taskRouteTrace = null;
                if (Utils.isNotEmpty(updaterouteTotask.getRouteLabel())) {
                    taskRouteTrace = "Task is routing to " + updaterouteTotask.getRouteLabel();
                }
                // update Switch or condition task task status to finish
                if (updaterouteTotask.getException() == null) {
                    List<Object> pram = new ArrayList<>();

                    // update task route to another task
                    if (Utils.isNotEmpty(updaterouteTotask.getRouteToTask())
                            && Utils.isNotEmpty(updaterouteTotask.getParam())) {

                        String Sql = "Update " + row.get("processTableName")
                                + " SET status_id = 4 , ACTUALENDDATE =localtimestamp , ASSIGNMENT_RULE_TRACE = ? "
                                + " where RECID = ?";
                        pram.add(taskRouteTrace);
                        pram.add(row.get("recid"));

                        SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(Sql,
                                SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);

                        // added by Mahmoud for check in
                        tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingSwitchCondtionTasks;
                        tx.TransactionKey = TransactionKey;

                        txs.add(tx);
                        String updaterouteTotaskP;
                        if (row.get("parentplantaskid") != null) {
                            updaterouteTotaskP = updaterouteTotask.getRouteToTask() + " AND PARENT_TASK_ID = " + row.get("parent_task_id") + " AND PARENTPLANTASKID = " + row.get("parentplantaskid");
                        } else {
                            updaterouteTotaskP = updaterouteTotask.getRouteToTask() + " AND PARENT_TASK_ID = " + row.get("parent_task_id");
                        }
                        SaveThreadTransactionInformation updaterouteTotaskTx = new SaveThreadTransactionInformation(
                                updaterouteTotaskP,
                                SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql,
                                updaterouteTotask.getParam());

                        txs.add(updaterouteTotaskTx);
                    } else {
                        // to handle case of no route valid depend on condition
                        // update status to 21 which mean no root valid

                        String Sql = "Update " + row.get("processTableName")
                                + " SET status_id = 21 , ACTUALENDDATE =localtimestamp where RECID = ?";
                        pram.add(row.get("recid"));

                        SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(Sql,
                                SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);

                        // added by Mahmoud for check in
                        tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingProcessTask;
                        tx.TransactionKey = TransactionKey;

                        txs.add(tx);

                    }
                } else {
                    List<Object> pram = new ArrayList<>();
                    String Sql = "Update " + row.get("processTableName")
                            + " SET status_id = 20 , ACTUALENDDATE =localtimestamp , ASSIGNMENT_RULE_TRACE = ?"
                            + " where RECID = ?";
                    pram.add(taskRouteTrace);
                    pram.add(row.get("recid"));

                    SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(Sql,
                            SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);

                    // added by Mahmoud for check in
                    tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingSwitchCondtionTasks;
                    tx.TransactionKey = TransactionKey;

                    txs.add(tx);

                    String updaterouteTotaskP;
                    if (row.get("parentplantaskid") != null) {
                        updaterouteTotaskP = updaterouteTotask.getRouteToTask() + " AND PARENT_TASK_ID = " + row.get("parent_task_id") + " AND PARENTPLANTASKID = " + row.get("parentplantaskid");
                    } else {
                        updaterouteTotaskP = updaterouteTotask.getRouteToTask() + " AND PARENT_TASK_ID = " + row.get("parent_task_id");
                    }
                    SaveThreadTransactionInformation updaterouteTotaskTx = new SaveThreadTransactionInformation(
                            updaterouteTotaskP,
                            SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql,
                            updaterouteTotask.getParam());

                    txs.add(updaterouteTotaskTx);

                    JobUtil.Debug("Rule Exception  ::=======////=======////=====/////===> "
                            + updaterouteTotask.getException().getMessage());
                }

                SaveThread.AddSaveThreadTransaction(txs);

            }

        } catch (NumberFormatException e) {
            NTGMessageOperation.PrintErrorTrace(e);
        } catch (RestClientException e) {
            NTGMessageOperation.PrintErrorTrace(e);
        }
    }

}
