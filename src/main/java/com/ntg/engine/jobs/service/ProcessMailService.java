package com.ntg.engine.jobs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.entites.EmailData;
import com.ntg.engine.entites.SendMail;
import com.ntg.engine.entites.ToDoList;
import com.ntg.engine.jobs.JobUtil;
import com.ntg.engine.service.serviceImpl.SendMailServiceImp;
import com.ntg.engine.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service()
public class ProcessMailService {


    @Autowired
    private CommonCachingFunction commonCachingFun;

    @Autowired
    private SendMailServiceImp sendmailService;

    @Value("${GetServerPublicURL}")
    String GetServerPublicURL;
    
    @Value("${email-dates-format}")
    String emailDatesFormat;


    public synchronized void ProcesssEmail(ToDoList todo, boolean isExpiryTask, String doneAction) {
        try {
            if (todo.getDynamic_mail() != null) {
                EmailData emailData = new EmailData();
                emailData.setToDoObject(todo);
                emailData.setCcList(new ArrayList<>());
                if (todo.getActionableEmail() != null) {
                    emailData.getCcList().add(todo.getDynamic_mail());
                    emailData.setEmail(todo.getActionableEmail());
                } else
                    emailData.setEmail(todo.getDynamic_mail());
                if(todo.getCcList() != null && !todo.getCcList().isEmpty())
                    emailData.getCcList().addAll(Arrays.asList(todo.getCcList().replace(";" , ",").split(",")));
                emailData.setTaskName(todo.getTaskname());
                emailData.setDueDate(todo.getReqCompletionDate());
                emailData.setTaskDescription(todo.getAssignDesc());
                sendMail(emailData, todo, isExpiryTask, doneAction);
                return;
            }

            List<String> emails = new ArrayList<>();
            List<String> ccList = new ArrayList<>();

            if (Utils.isNotEmpty(todo.getTo_EmployeeID())) {
                List<Long> empIds = new ArrayList<>();
                // adding assignee id
                empIds.add(todo.getTo_EmployeeID());
                // adding cclist ids
                if (Utils.isNotEmpty(todo.getCcList())) {
                    String[] emps = todo.getCcList().replace(";" , ",").split(",");
                    for (String emp : emps) {
                        if (isNumeric(emp)) {
                            Long id = JobUtil.convertBigDecimalToLong(new BigDecimal(emp));
                            empIds.add(id);
                        }
                    }
                }

                // added new service in sa_admin to handle getting more than one email in one
                // call and adding caching
                if (Utils.isNotEmpty(empIds)) {
                    List<String> empEmails = commonCachingFun.getEmployeesEmails(empIds);
                    if (Utils.isNotEmpty(empEmails)) {
                        emails.addAll(empEmails);
                    }
                }
            }

            // adding caching when getting group email
            if (Utils.isNotEmpty(todo.getTo_GroupID())) {
                String groupEmail = commonCachingFun.getGroupEmail(todo.getTo_GroupID());
                if (Utils.isNotEmpty(groupEmail))
                    emails.add(groupEmail);
            }

            if (Utils.isNotEmpty(todo.getTo_GroupID())) {
                String cc = commonCachingFun.GetGroupCCList(todo.getTo_GroupID());
                if (Utils.isNotEmpty(cc) && !cc.equalsIgnoreCase("null"))
                    ccList = Arrays.asList(cc.replace(";" , ",").split(","));
            }

            if(todo.getCcList() != null && !todo.getCcList().isEmpty()) {
                ccList.addAll(Arrays.asList(todo.getCcList().replace(";" , ",").split(",")));
            }


            for (String Email : emails) {
                if (Email != null) {
                    EmailData emailData = new EmailData();
                    emailData.setToDoObject(todo);
                    emailData.setTaskName(todo.getTaskname());
                    emailData.setDueDate(todo.getReqCompletionDate());
                    emailData.setTaskDescription(todo.getAssignDesc());
                    emailData.setCcList(new ArrayList<>(ccList));
                    //in-case of dynamic assignation
                    if(todo.getActionableEmail() != null) {
                        if(emailData.getCcList() == null)
                            emailData.setCcList(new ArrayList<>());
                        emailData.getCcList().add(Email);
                        emailData.setEmail(todo.getActionableEmail());
                    }else
                        emailData.setEmail(Email);
                    sendMail(emailData, todo, isExpiryTask, doneAction);
                }
            }

        } catch (Exception exp) {
            if (Utils.isNotEmpty(todo) && todo.taskTypeId == 10) {
                sendmailService.handleEmailTaskRoute(todo, exp);
            }
            String txID;
            if (todo.getRecId() != null && todo.getRecId() > 0) {
                txID = "ToDoList_ID=" + todo.getRecId();
            } else {
                txID = todo.udaName + "/T_ID:" + todo.taskID + "_O_ID:" + todo.object_Id;
            }

            System.out.println(txID + ": Processing Sending Email Fail");
            NTGMessageOperation.PrintErrorTrace(exp, txID);
        }
    }


    // handle Mail if normal or template Email task By Abdulrahman Helal
    // updated By Mahmoud
    private synchronized void sendMail(EmailData email, ToDoList row, boolean isExpiryTask, String doneAction) throws Exception {


        if ((email.getToDoObject().getEmailTemplateId() == null
                || (email.getToDoObject().getEmailTemplateId() != null && row.sendEmailonAssign) || isExpiryTask)
                && (row.taskTypeId == 1 || row.taskTypeId == 4)) {
            // send mail for human task if not email template or email task
        	StringBuilder body = new StringBuilder("<table border: 3px;border-collapse: collapse border-spacing: 4px;>");

            if (isExpiryTask) {
                body.append(" <tr>Your task exceeded expiration date, So it has been done automatically. </tr>");
            } else {
                body.append(" <tr><th> You have a new Task Assigned  </th></tr>");
            }

            if (email.getTaskName() != null) {
                body.append(" <tr>    <td> Task Name is :   </td>  <td> ");
                body.append(email.getTaskName());
                body.append("</td>");
                body.append(" </tr>");
            }
            ///need review again
            //<yghandor> usually description is resolved in pending task handler
            if (email.getTaskDescription() != null) {
                if (email.getTaskDescription().contains("{{") && email.getTaskDescription().contains("}}")) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> map = objectMapper.convertValue(row, Map.class);
                    Map ObjectOfProcess = commonCachingFun.loadCrmObject(map, null, null, row.getCompanyName());
                    String description = commonCachingFun.resolveDescription(map, email.getTaskDescription(), null,
                            null, ObjectOfProcess,
                            false, row.getCompanyName(), row.getTenantSchema());
                    body.append(" <tr><td>Task Description is :</td><td>");
                    body.append(description);
                    body.append("</td>");
                    body.append(" </tr>");
                } else {
                    body.append("<tr><td> Task Description is :</td><td>");
                    body.append(email.getTaskDescription());
                    body.append("</td>");
                    body.append("</tr>");
                }
            }

            if (isExpiryTask && doneAction != null) {
                body.append("<tr><td> Done Action is :</td><td> ");
                body.append(doneAction);
                body.append("</td>  </tr>");
            }

            if (email.getDueDate() != null && !isExpiryTask) {
                body.append(" <tr>    <td> Due Date For task :  </td>    <td> ");
                body.append(Utils.formatDate(new Date(email.getDueDate().getTime()), emailDatesFormat));
                body.append("</td>  </tr>");
            }
            //adding link for system

            body.append("<tr>    <td><hr> </td>    <td><hr> </td>  </tr> <tr>    <td> </td>    <td> <a href=\"");
            body.append(GetServerPublicURL.split("/rest/")[0]).append("/#/pages/todolist/myTasks");
            body.append("\" style=\"margin-right: 30px;background-color: #0078d7;color: white;padding: 0.5em 1em;text-decoration: none;font-weight: 600;border-radius: 5px;\">Open The System</a></td>  </tr>");


            body.append("</table>");
            //adding link for system


            SendMail sendMailData = new SendMail();

            sendMailData.setSendTo(email.getEmail());
            if(email.getCcList() != null)
                sendMailData.setCCList(email.getCcList().stream()
                        .distinct()
                        .collect(Collectors.toList()));

            if (isExpiryTask) {
                sendMailData.setSubject("Expiration Notification");
            } else {
                sendMailData.setSubject("New Task [" + email.getTaskName() + "]");
            }
            sendMailData.setBody(body.toString());


            if (row.taskTypeId == 1 && email.getToDoObject().getEmailTemplateId() != null && !isExpiryTask) {
                sendmailService.handleSendingEmailTemplate(row, email.getToDoObject().getEmailTemplateId(),
                        email.getToDoObject().getTypeid(), email.getToDoObject().getObject_Id(), email.getEmail(),
                        true, row.getCompanyName(), row.getTenantSchema(), true,
                        sendMailData.getCCList());
            } else {
                sendmailService.sendMail(sendMailData , null);
            }

        } else {
            if (row.getDynamic_mail() != null) {
                sendmailService.handleSendingEmailTemplate(row, email.getToDoObject().getEmailTemplateId(),
                        email.getToDoObject().getTypeid(), email.getToDoObject().getObject_Id(),
                        row.getDynamic_mail(), true, row.getCompanyName(), row.getTenantSchema(), false, email.getCcList());
            } else
                // handle send email template
                sendmailService.handleSendingEmailTemplate(row, email.getToDoObject().getEmailTemplateId(),
                        email.getToDoObject().getTypeid(), email.getToDoObject().getObject_Id(), email.getEmail(),
                        true, row.getCompanyName(), row.getTenantSchema(), false, email.getCcList());
        }


    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
