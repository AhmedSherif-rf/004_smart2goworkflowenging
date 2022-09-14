package com.ntg.engine.jobs;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.entites.ToDoList;
import com.ntg.engine.jobs.service.CommonCachingFunction;
import com.ntg.engine.jobs.service.ProcessMailService;
import com.ntg.engine.service.JobService;
import com.ntg.engine.util.ArchievedTask;
import com.ntg.engine.util.SaveDoneTaskReq;
import com.ntg.engine.util.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

public class expiryTasksHandler implements Job {


    private String jobName;


    @Autowired
    private JobService jobService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CommonCachingFunction commonCachingFun;

    @Autowired
    private ProcessMailService processMailService;

    @Value("${doneTasksURL}")
    String doneTasksURL;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        jobName = context.getJobDetail().getKey().toString().split("\\.")[1];
        JobUtil.Debug("==============Start ExpiryTask Handler ============== <" + jobName + ">");
        try {
            Map<String, Object> row = JobUtil.CheckOutExpiryTask();

            while (row != null) {
                String companyName = (Utils.isNotEmpty(row.get("companyName"))) ? row.get("companyName").toString() : "NTG";
                processProcessTask(row, companyName);

                row = JobUtil.CheckOutExpiryTask();
            }

        } catch (Exception e) {
            NTGMessageOperation.PrintErrorTrace(e);

        }
        jobService.deleteJob(jobName);
        // JobUtil.Debug("==============End PendingProcessTask ============== <" + jobName + ">");

    }

    private void processProcessTask(Map<String, Object> row, String companyName) throws Exception {

        //Request URL: http://127.0.0.1:3000/rest/toDo/handleSaveDoneTask

        try {
            String doneAction = (String) row.get("expiration_action");
            SaveDoneTaskReq saveDoneTaskReq = new SaveDoneTaskReq();
            mapDoneRequest(row, saveDoneTaskReq, companyName);

            HttpHeaders headersRule = new HttpHeaders();
            headersRule.setContentType(MediaType.APPLICATION_JSON_UTF8);

            headersRule.set("SessionToken", commonCachingFun.BackEndLogin(companyName));
            headersRule.set("User-Agent", "Smart2GoWorkFlowEngine");

            HttpEntity<SaveDoneTaskReq> entity = new HttpEntity<SaveDoneTaskReq>(saveDoneTaskReq, headersRule);

            ResponseEntity<Object> responseEntity2 = restTemplate.exchange(doneTasksURL, HttpMethod.POST, entity, Object.class);


            processMailService.ProcesssEmail(saveDoneTaskReq.getDoneTask(), true, doneAction);
        } catch (Exception ex) {
            System.out.println("War : Done Expiry Task Expression " + ex.getMessage());
        }

        long taskrecid = JobUtil.convertBigDecimalToLong(row.get("recid"));
        String expiryTaskKey = "e" + taskrecid;
        JobUtil.CheckInExpiryTask(expiryTaskKey);


    }


    private void mapDoneRequest(Map<String, Object> row, SaveDoneTaskReq saveDoneTaskReq, String companyName) throws InterruptedException {

        ToDoList toDoList = new ToDoList();
        ArchievedTask archivedTask = new ArchievedTask();

        toDoList.setOpId((Long) row.get("op_id"));
        toDoList.setRecId((Long) row.get("recid"));
        toDoList.setTypeid((Long) row.get("typeid"));
        toDoList.setObject_Id((Long) row.get("object_id"));
        toDoList.setTaskID((Long) row.get("task_id"));
        toDoList.setFromMangerialLevel((Long) row.get("from_mangerial_level"));
        toDoList.setToMangerialLevel((Long) row.get("to_mangerial_level"));
        toDoList.setTo_EmployeeID((Long) row.get("to_employee_id"));
        toDoList.setNextApprovalIf((String) row.get("next_approval_if"));
        toDoList.setParentTaskId((Long) row.get("parent_task_id"));
        toDoList.setIsAddHock((Boolean) row.get("is_add_hock"));
        toDoList.setIschild((Boolean) row.get("is_child"));
        toDoList.setMinimumNumOfApproval((Long) row.get("minimum_num_of_approval"));
        //
        toDoList.setAssignDate((Timestamp) row.get("assign_date"));
        toDoList.setDoneDate(new Timestamp(System.currentTimeMillis()));
        toDoList.setIncom_RecID((Long) row.get("incom_recid"));
        toDoList.setAssignDesc((String) row.get("assign_desc"));
        toDoList.setDynamic_mail((String) row.get("dynamic_mail"));
        toDoList.setModuleId((Long) row.get("module_id"));
        toDoList.setTaskname((String) row.get("taskname"));
        toDoList.setSubStatusTaskID((Long) row.get("sub_status_taskId"));
        toDoList.setTaskTypeId((Long) row.get("task_type_id"));
        toDoList.setToDepName((String) row.get("to_dep_name"));
        toDoList.setPriority((String) row.get("priority"));
        toDoList.setPriorityColor((String) row.get("priority_color"));
        toDoList.setUdaName((String) row.get("uda_name"));
        toDoList.setTo_GroupID((Long) row.get("to_group_id"));
        toDoList.setCcList((String) row.get("cc_list"));

        toDoList.setReqCompletionDate((Timestamp) row.get("req_completion_date"));

        toDoList.setOrderedMembersImagesIds((String) row.get("ordered_members_images_ids"));

        archivedTask.setTask(toDoList);
        archivedTask.setUdas(null);

        saveDoneTaskReq.setDoneTask(toDoList);
        saveDoneTaskReq.setArchTask(archivedTask);
        saveDoneTaskReq.setDate(new Date().toString());
        saveDoneTaskReq.setRoute_if((String) row.get("expiration_action"));
    }

}
