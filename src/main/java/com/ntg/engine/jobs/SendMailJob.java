package com.ntg.engine.jobs;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.entites.ToDoList;
import com.ntg.engine.jobs.service.CommonCachingFunction;
import com.ntg.engine.jobs.service.ProcessMailService;
import com.ntg.engine.jobs.service.PushNotficationService;
import com.ntg.engine.service.serviceImpl.JobServiceImpl;
import com.ntg.engine.util.Employee;
import com.ntg.engine.util.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Abdulrahman Helal Updated By Mahmoud
 */
public class SendMailJob implements Job {

    @Autowired
    private JobServiceImpl jobService;

    @Autowired
    private CommonCachingFunction commonCachingFun;

    @Autowired
    private PushNotficationService _pushNotficationService;

    @Autowired
    private ProcessMailService processMailService;

    @Value("${wf_pendingMailPoolSize}")
    String wf_pendingMailPoolSize;

    String jobName;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            jobName = context.getJobDetail().getKey().toString().split("\\.")[1];
            JobUtil.Debug("==============Start SendMailJob ============== <" + jobName + ">");

            ToDoList row = JobUtil.CheckOutMailTask();

            while (row != null) {

                processMailService.ProcesssEmail(row, false, null);


                if (row.to_EmployeeID != null && row.to_EmployeeID > 0) {
                    this._pushNotficationService.sendTaskPushNotfication(row.assignDesc, row.to_EmployeeID, row.getRecId(), row.getCompanyName());
                }
//@AddedBy:Aya.Ramadan => Dev-00002285:Push notification to group members
                if (row.getTo_GroupID() != null && row.getTo_GroupID() > 0) {
                    List<Long> empIds = new ArrayList<>();
                    Employee[] employees = commonCachingFun.getAllGroupEmployees(row.getTo_GroupID());
                    if (Utils.isNotEmpty(employees)) {
                        for (Employee emp : employees) {
                            empIds.add(emp.getUserID());
                        }
                    }
                    this._pushNotficationService.sendTaskPushNotficationToGroup(row.assignDesc, empIds, row.getRecId(), row.getCompanyName());
                }
                row = JobUtil.CheckOutMailTask();
            }
        } catch (Exception e) {
            NTGMessageOperation.PrintErrorTrace(e);
        }
        jobService.deleteJob(jobName);
        JobUtil.Debug("==============End SendMailJob ============== <" + jobName + ">");

    }

}
