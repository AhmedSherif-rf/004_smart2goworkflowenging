/*
 *
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER. Copyright 1997-2020 NTG Clarity and/or its affiliates. All
 *
 *  rights reserved. NTG CLARITY is a leader in delivering network, telecom, IT and infrastructure solutions to network
 *
 *  service providers and medium and large enterprises. www.ntgclarity.com The contents of this file are subject to the
 *
 *  terms of "NTG Clarity License". You must not use this file except in compliance with the License. You can obtain a
 *
 *  copy of the License at http://www.ntgclarity.com/ See the License for the specific language governing permissions and
 *
 *  limitations under the License. Contributor(s): The Initial Developer of the Original Software is NTG Clarity . , Inc.
 *
 *  Copyright 1997-2020 NTG Clarity. All Rights Reserved. CLASS NAME <h4>Description</h4> <h4>Notes</h4>
 *
 *  <h4>References</h4>
 *
 *  @author: ${CITIES} <A HREF="mailto:[ntg.support@ntgclarity.com]">NTG Clarity Support Team</A>
 *
 *  @version Revision: 1.0.1 Date: ${date} ${time}
 *
 *  @see [String]
 *
 *  @see [URL]
 *
 *  @see [Class name#method name]
 *
 * /
 */

package com.ntg.engine.jobs;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.jobs.service.CommonCachingFunction;
import com.ntg.engine.service.JobService;
import com.ntg.engine.util.LoginSettings;
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

import java.util.*;

/**
 * @author Abdulrahman Helal Updated By Mahmoud Atef
 */
public class PendingProcessTask implements Job {

    @Autowired
    private LoginSettings login;

    @Autowired
    private CommonCachingFunction commonCachingFun;

    @Autowired
    private JobService jobService;

    @Autowired
    private RestTemplate restTemplate;

    String jobName;

    // for service check rules that invoke rule
    @Value("${wf_checkRules}")
    String wf_checkRules;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        jobName = context.getJobDetail().getKey().toString().split("\\.")[1];
        JobUtil.Debug("==============Start PendingProcessTask ============== <" + jobName + ">");
        try {
            Map<String, Object> row = JobUtil.CheckOutPendingProcessTask();

            while (row != null) {
                String companyName = (Utils.isNotEmpty(row.get("companyName"))) ? row.get("companyName").toString() : "NTG";
                processProcessTask(row, companyName);

                row = JobUtil.CheckOutPendingProcessTask();
            }

        } catch (Exception e) {
            NTGMessageOperation.PrintErrorTrace(e);

        }
        jobService.deleteJob(jobName);
        // JobUtil.Debug("==============End PendingProcessTask ============== <" + jobName + ">");
    }

    /**
     * @param row
     * @throws Exception
     */
	public void processProcessTask(Map<String, Object> row, String companyName) throws Exception {

        try {

            Long typeId = Long.parseLong(row.get("TYPE_ID").toString());

            List<SaveThreadTransactionInformation> txs = new ArrayList<SaveThreadTransactionInformation>();

            // set operation Id
            String TransactionKey = JobUtil.CreateProcessTasksKey(row);

            HttpHeaders headersRule = new HttpHeaders();
            headersRule.setContentType(MediaType.APPLICATION_JSON);
            headersRule.set("SessionToken", commonCachingFun.BackEndLogin(companyName));
            headersRule.set("User-Agent", "Smart2GoWorkFlowEngine");

            List<Map<String, Object>> listToBeSent = new ArrayList<Map<String, Object>>();
            Map<String, Object> mapToBeSent = new Hashtable<String, Object>();
            mapToBeSent.put("recid", Long.parseLong(row.get("OBJECT_ID").toString()));
            mapToBeSent.put("taskId", Long.parseLong(row.get("TASKID").toString()));
            mapToBeSent.put("tableName", row.get("processTableName"));
            mapToBeSent.put("doneAction", "");

            listToBeSent.add(mapToBeSent);

            String InvokeRule = login.getUrl() + "/rest/Schedule/SMS/" + 0 + "/" + typeId + "/" + true + "/" + true;

            for (Map<String, Object> eachList : listToBeSent) {
                HttpEntity<Map<String, Object>> entityRule = new HttpEntity<>(eachList, headersRule);
                ResponseEntity<UpdateRouteToTask> responseEntity = restTemplate.exchange(InvokeRule, HttpMethod.POST,
                        entityRule, UpdateRouteToTask.class);
                UpdateRouteToTask updaterouteTotask = responseEntity.getBody();

                // update process task status to finish
                if (updaterouteTotask.getException() == null) {
                    List<Object> pram = new ArrayList<>();
                    // update task route to another task
                    if (Utils.isNotEmpty(updaterouteTotask.getRouteToTask())
                            && Utils.isNotEmpty(updaterouteTotask.getParam())) {

                        String Sql = "Update " + row.get("processTableName")
                                + " SET status_id = 4 , ACTUALENDDATE =localtimestamp where RECID = ?";
                        pram.add(row.get("recid"));
                        SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(Sql,
                                SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);

                        Long recid = (row.get("analytics_recid") == null) ? null
                                : Long.parseLong(row.get("analytics_recid").toString());
                        if (recid != null) {

                            String sql3 = "Update " + row.get("analyticsTableName") + " SET END_TIME = ? WHERE TASK_ID = ?";
                            ArrayList<Object> anpram = new ArrayList<>();
                            anpram.add(new Date());
                            anpram.add(row.get("recid"));
                            SaveThreadTransactionInformation tx2 = new SaveThreadTransactionInformation(sql3,
                                    SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, anpram);

                            // added by Mahmoud for check in
                            tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingProcessTask;

                            txs.add(tx2);


                            String sql4 = "Update " + row.get("analyticsTableName") + " set REAL_TIME = ("
                                    + "EXTRACT(SECOND FROM(end_time - start_time)) + "
                                    + "( EXTRACT(MINUTE FROM(end_time - start_time)) * 60 ) + "
                                    + "( EXTRACT(hour FROM(end_time - start_time)) * 60*60 ) + "
                                    + "( EXTRACT(day FROM(end_time - start_time)) * 60*60*24 ) )" + " where TASK_ID = ?";

                            anpram = new ArrayList<>();
                            anpram.add(row.get("recid"));
                            tx2 = new SaveThreadTransactionInformation(sql4,
                                    SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, anpram);

                            // added by Mahmoud for check in
                            tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingProcessTask;

                            txs.add(tx2);


                        }
                        // added by Mahmoud for check in
                        tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingProcessTask;
                        tx.TransactionKey = TransactionKey;

                        txs.add(tx);
                        String updaterouteTotaskP;
                        if (row.get("parentplantaskid") != null) {
                            updaterouteTotaskP = updaterouteTotask.getRouteToTask() + " AND PARENT_TASK_ID = "
                                    + row.get("parent_task_id") + " AND PARENTPLANTASKID = "
                                    + row.get("parentplantaskid");
                        } else {
                            updaterouteTotaskP = updaterouteTotask.getRouteToTask() + " AND PARENT_TASK_ID = "
                                    + row.get("parent_task_id");
                        }
                        SaveThreadTransactionInformation updaterouteTotaskTx = new SaveThreadTransactionInformation(
                                updaterouteTotaskP, SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql,
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
                    // ===update process task status to exception
                    List<Object> pram = new ArrayList<>();
                    String Sql = "Update " + row.get("processTableName")
                            + " SET status_id = 20 , ACTUALENDDATE =localtimestamp where RECID = ?";
                    pram.add(row.get("recid"));
                    SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(Sql,
                            SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);

                    // added by Mahmoud for check in
                    tx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingProcessTask;
                    tx.TransactionKey = TransactionKey;

                    txs.add(tx);

                    String updaterouteTotaskP;
                    if (row.get("parentplantaskid") != null) {
                        updaterouteTotaskP = updaterouteTotask.getRouteToTask() + " AND PARENT_TASK_ID = "
                                + row.get("parent_task_id") + " AND PARENTPLANTASKID = " + row.get("parentplantaskid");
                    } else {
                        updaterouteTotaskP = updaterouteTotask.getRouteToTask() + " AND PARENT_TASK_ID = "
                                + row.get("parent_task_id");
                    }
                    SaveThreadTransactionInformation updaterouteTotaskTx = new SaveThreadTransactionInformation(
                            updaterouteTotaskP, SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql,
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
