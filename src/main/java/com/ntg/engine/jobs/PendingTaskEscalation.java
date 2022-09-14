package com.ntg.engine.jobs;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.jobs.service.CommonCachingFunction;
import com.ntg.engine.repository.customImpl.SqlHelperDaoImpl;
import com.ntg.engine.service.JobService;
import com.ntg.engine.service.serviceImpl.SendMailServiceImp;
import com.ntg.engine.util.EmployeeInfo;
import com.ntg.engine.util.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Mahmoud Atef
 */

public class PendingTaskEscalation implements Job {

    @Autowired
    SqlHelperDaoImpl sqlHelperDao;

    @Autowired
    private JobService jobService;

    @Autowired
    private SendMailServiceImp sendMailServiceImp;

    @Autowired
    private CommonCachingFunction commonCachingFun;


    String jobName;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        jobName = context.getJobDetail().getKey().toString().split("\\.")[1];
        JobUtil.Debug("==============Start PendingTaskEscalation============== <" + jobName + ">");

        try {

            Map<String, Object> row = JobUtil.CheckOutPendingTaskEscalation();

            if (row == null) {
                JobUtil.Debug("<" + jobName + "> Didn't find Escalation Row To Process ... ");
            }
            while (row != null) {
                String companyName = (Utils.isNotEmpty(row.get("companyName"))) ? row.get("companyName").toString() : "NTG";
                String tenantSchema = (Utils.isNotEmpty(row.get("tenantSchema"))) ? row.get("tenantSchema").toString() : "";
                CheckEscalatedTask(row, companyName, tenantSchema);

                row = JobUtil.CheckOutPendingTaskEscalation();
            }

        } catch (Exception e) {
            NTGMessageOperation.PrintErrorTrace(e);

        }

        jobService.deleteJob(jobName);
        //	JobUtil.Debug("==============End PendingTaskEscalation ============== <" + jobName + ">");

    }

    public void CheckEscalatedTask(Map<String, Object> row, String companyName, String tenantSchema) throws Exception {

        List<SaveThreadTransactionInformation> txs = new ArrayList<>();

        // set operation Id
        String TransactionKey = JobUtil.CreateEscalationTaskKey(row);

        if (row != null) {
            Long typeId = null, escalatedToId = null;
            boolean groupFlag = false;
            Map<String, Object> uda = (Map<String, Object>) row.get("uda");

            Long object_id = JobUtil.convertBigDecimalToLong(row.get("object_id"));
            Long orginatorId = JobUtil.convertBigDecimalToLong(row.get("created_by_id"));
            String escTableName = row.get("escHistoryTableName").toString();

            if (uda != null) {
                typeId = JobUtil.convertBigDecimalToLong(uda.get("type_id"));
            }
            Long primeTypeId = JobUtil.convertBigDecimalToLong(row.get("prime_type_id"));

            if (Utils.isNotEmpty(primeTypeId)) {
                Long escPrimeId = 0L;
                if (Utils.isNotEmpty(row.get("prime_id"))) {
                    escPrimeId = JobUtil.convertBigDecimalToLong(row.get("prime_id"));
                }
                if (primeTypeId == 0) { // escalate to originator
                    escalatedToId = orginatorId;
                } else if (primeTypeId == 1) { // escalate to manager
                    EmployeeInfo[] managerList = commonCachingFun.GetManagerInfo(orginatorId, 1L);
                    if (Utils.isNotEmpty(managerList)) {
                        EmployeeInfo info = managerList[0];
                        escalatedToId = info.Emp_ID;
                    } else {
                        escalatedToId = orginatorId;
                    }
                } else if (primeTypeId == 2) { // escalate to group
                    escalatedToId = escPrimeId;
                    groupFlag = true;
                } else if (primeTypeId == 3) { // escalate to employee
                    escalatedToId = escPrimeId;
                } else if (primeTypeId == 4) { // escalate to assignment rules
                    Map<String, Object> escalatedMap = commonCachingFun.checkAssignmentRule(escPrimeId, typeId,
                            object_id, null, orginatorId, true, companyName);

                    escalatedToId = JobUtil.convertBigDecimalToLong(escalatedMap.get("escalatedToId"));
                }
            }

            if (Utils.isNotEmpty(escalatedToId)) {
                // get email to send to
                String toEmail = null;
                List<String> cc = new ArrayList<>();
                if (groupFlag == true) {
                    toEmail = commonCachingFun.getGroupEmail(escalatedToId);
                    String ccString = commonCachingFun.GetGroupCCList(escalatedToId);
                    if(ccString != null && !ccString.isEmpty())
                        cc = Arrays.asList(ccString.replace(";" , ",").split(","));
                } else {
                    List<Long> ids = new ArrayList<Long>();
                    ids.add(escalatedToId);
                    List<String> empEmails = commonCachingFun.getEmployeesEmails(ids);
                    if (Utils.isNotEmpty(empEmails)) {
                        toEmail = empEmails.get(0);
                    }
                }

                // Handle Email Template Sending with the new Configuration and add now column
                // to set trace
				Long emailTempleteId = JobUtil.convertBigDecimalToLong(row.get("email_template_id"));
				String emailResponse = sendMailServiceImp.handleSendingEmailTemplate(null, emailTempleteId, typeId,
						object_id, toEmail, false, companyName, tenantSchema, false , cc);

				StringBuilder insertQueryBuilder = new StringBuilder("");
				insertQueryBuilder = insertQueryBuilder.append("INSERT INTO " + escTableName
						+ "( recid , escalation_id, task_id, escalatedToId, uda_id, type_id, escalation_trace ) VALUES ( ? ,? ,? ,? ,? ,? ,? )");
				List<Object> params = new ArrayList<>();

				Long recId = sqlHelperDao.FtechRecID(escTableName + "_s");
				Long udaId = JobUtil.convertBigDecimalToLong(uda.get("recid"));
				Long escalationId = JobUtil.convertBigDecimalToLong(row.get("recid"));
				Long planTaskId = JobUtil.convertBigDecimalToLong(row.get("plantaskid"));

                params.add(recId);
                params.add(escalationId);
                params.add(planTaskId);
                params.add(escalatedToId);
                params.add(udaId);
                params.add(typeId);
                params.add(emailResponse); // added to add trace

                SaveThreadTransactionInformation updateParamTx = new SaveThreadTransactionInformation(
                        insertQueryBuilder.toString(), SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql,
                        params);

                updateParamTx.TransactionSource = SaveThreadTransactionInformation.TxSources.PendingTaskEscalation;
                updateParamTx.TransactionKey = TransactionKey;

                txs.add(updateParamTx);

                SaveThread.AddSaveThreadTransaction(txs);
            }
        }
    }

}
