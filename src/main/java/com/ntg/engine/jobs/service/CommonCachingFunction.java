package com.ntg.engine.jobs.service;

import com.ntg.common.NTGEHCacher;
import com.ntg.engine.entites.HumanTaskActions;
import com.ntg.engine.entites.SendMail;
import com.ntg.engine.entites.StringResponse;
import com.ntg.engine.entites.ToDoList;
import com.ntg.engine.jobs.JobUtil;
import com.ntg.engine.jobs.SaveThread;
import com.ntg.engine.jobs.SaveThreadTransactionInformation;
import com.ntg.engine.repository.customImpl.SqlHelperDaoImpl;
import com.ntg.engine.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

@Service()
public class CommonCachingFunction {

    static NTGEHCacher<String, Object> CachingCommonData;

    static {
        CachingCommonData = new NTGEHCacher<String, Object>(Setting.CacheEmployeeInfoFor);
    }

    @Value("${wf_GetEmployee}")
    String wf_GetEmployee;

    @Value("${crm_GetEmployeeMangers}")
    String GetEmployeeMangers;

    @Value("${wf_GetEmployeesEmails}")
    String wf_GetEmployeesEmails;

    @Value("${wf_GetGroupEmail}")
    String wf_GetGroupEmail;

    @Value("${wf_GetGroupCCList}")
    String wf_GetGroupCCList;

    @Value("${GroupMembers}")
    String GroupMembers;

    @Value("${checkAssignmentRules_new}")
    String checkAssignmentRule;

    @Value("${wf_loadObject}")
    String wf_loadObject;
    @Value("${wf_loadChartFields}")
    String wf_loadChartFields;

    @Value("${GetEmailTemplateByID}")
    String GetEmailTemplateByID;

    @Value("${GetServerPublicURL}")
    String GetServerPublicURL;

    @Value("${GetOrderedGridColumnsURL}")
    String GetOrderedGridColumnsURL;

    @Value("${wf_loadAttachments}")
    String wf_loadAttachments;

    private static final String UDA_TYPE_KEY = "udaType";
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SqlHelperDaoImpl sqlHelperDao;

    @Autowired
    private LoginSettings loginSettings;


    public EmployeeFullInfoResponse getEmployeeFullInfo(Long EmploeeID) {
        String key = "Emp_" + EmploeeID;
        EmployeeFullInfoResponse response = (EmployeeFullInfoResponse) CachingCommonData.get(key);
        if (response == null) {
            EmployeeFullInfoRequest req = new EmployeeFullInfoRequest();
            req.empId = EmploeeID;

            ResponseEntity<EmployeeFullInfoResponse> responseEntity = restTemplate.postForEntity(wf_GetEmployee, req,
                    EmployeeFullInfoResponse.class);
            if (responseEntity.getBody() != null) {
                response = responseEntity.getBody();
                CachingCommonData.put(key, response);
            }
        }
        return response;
    }

    public EmployeeInfo[] GetManagerInfo(Long EmployeeId, Long ManagerLevel /* one mean direct manager */) {
        String key = "Manager_" + EmployeeId + "_" + ManagerLevel;
        EmployeeInfo[] managerInfo = (EmployeeInfo[]) CachingCommonData.get(key);
        if (Utils.isEmpty(managerInfo)) {
            Map<String, Long> objMap = new HashMap<String, Long>();
            objMap.put("empId", EmployeeId);
            objMap.put("managerLevel", ManagerLevel);

            ResponseEntity<UserResponse> responseEntity = restTemplate.postForEntity(GetEmployeeMangers, objMap,
                    UserResponse.class);
            UserResponse userResponse = responseEntity.getBody();
            if (userResponse != null) {
                managerInfo = userResponse.returnValue;
                CachingCommonData.put(key, managerInfo);
            }
        }
        return managerInfo;
    }

    @SuppressWarnings("unchecked")
    public List<String> getEmployeesEmails(List<Long> employeesIds) {
        String key = "Emp_Email";
        for (int i = 0; i < employeesIds.size(); i++) {
            key += '_' + employeesIds.get(i);
        }
        List<String> employeesEmails = (List<String>) CachingCommonData.get(key);
        if (Utils.isEmpty(employeesEmails)) {
            ResponseEntity<GetEmailsResponse> responseEntity = restTemplate.postForEntity(wf_GetEmployeesEmails,
                    employeesIds, GetEmailsResponse.class);
            GetEmailsResponse emails = responseEntity.getBody();
            if (Utils.isNotEmpty(emails)) {
                employeesEmails = emails.getEmails();
                CachingCommonData.put(key, employeesEmails);
            }
        }
        return employeesEmails;
    }

    public String getGroupEmail(Long groupId) {
        String key = "Group_Email_" + groupId;
        String groupEmail = (String) CachingCommonData.get(key);
        if (Utils.isEmpty(groupEmail)) {
            ResponseEntity<StringResponse> responseEntity = restTemplate.postForEntity(wf_GetGroupEmail, groupId, StringResponse.class);
            if (Utils.isNotEmpty(responseEntity.getBody()) && !Utils.isEmptyString(responseEntity.getBody().returnValue)) {
                groupEmail = responseEntity.getBody().returnValue;
                CachingCommonData.put(key, groupEmail);
            }
        }
        return groupEmail;
    }

    public String GetGroupCCList(Long groupId) {
        String key = "Group_CC_" + groupId;
        String groupCCList = (String) CachingCommonData.get(key);
        if (Utils.isEmpty(groupCCList)) {
            ResponseEntity<StringResponse> responseEntity = restTemplate.postForEntity(wf_GetGroupCCList, groupId, StringResponse.class);
            if (Utils.isNotEmpty(responseEntity.getBody()) && !Utils.isEmptyString(responseEntity.getBody().returnValue)) {
                groupCCList = responseEntity.getBody().returnValue;
                CachingCommonData.put(key, groupCCList);
            }
        }
        return groupCCList;
    }

    public Employee[] getAllGroupEmployees(Long groupId) {
        String key = "Group_Emps_" + groupId;
        Employee[] employees = (Employee[]) CachingCommonData.get(key);

        if (Utils.isEmpty(employees)) {
            GetGroupMembersReq req = new GetGroupMembersReq();
            req.RecID = groupId;

            ResponseEntity<GetGroupMembersRes> responseEntity = restTemplate.postForEntity(GroupMembers, req,
                    GetGroupMembersRes.class);
            GetGroupMembersRes messagesRes = responseEntity.getBody();
            if (messagesRes != null) {
                employees = messagesRes.returnValue;
                CachingCommonData.put(key, employees);
            }
        }
        return employees;
    }

    @SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
    public Map<String, Object> checkAssignmentRule(Long templateId, Long typeId, Long object_id, String created_by,
                                                   Long orginatorId, Boolean isEcalation, String companyName) throws Exception {

        String loginToken = BackEndLogin(companyName);
        HttpHeaders headersRule = new HttpHeaders();
        headersRule.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headersRule.set("SessionToken", loginToken);
        headersRule.set("User-Agent", "Smart2GoWorkFlowEngine");

        // String key = "Check_Rule_" + typeId + "_" + templateId;
        // Map<String, Object> assigneeObj = (Map<String, Object>)
        // CachingCommonData.get(key);

        Map<String, Object> assigneeObj = null;

        // if (Utils.isEmpty(assigneeObj)) {

        Map<String, Object> row = new HashMap<String, Object>();
        row.put("templateId", templateId);
        row.put("typeId", typeId);
        row.put("object_id", object_id);
        row.put("orginatorId", orginatorId);
        row.put("created_by", created_by);
        row.put("isEcalation", isEcalation);

        HttpEntity<Map<String, Object>> entityRule = new HttpEntity<>(row, headersRule);
        ResponseEntity<Map> responseEntity = restTemplate.exchange(checkAssignmentRule, HttpMethod.POST, entityRule,
                Map.class);
        if (Utils.isNotEmpty(responseEntity.getBody())) {
            assigneeObj = responseEntity.getBody();
        }
        // CachingCommonData.put(key, assigneeObj);
        // }
        return assigneeObj;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> getTaskEmailTemplate(Long emailTemplateId, String companyName) throws InterruptedException {
        String key = "Email_Template_" + emailTemplateId;
        Map<String, Object> emailTemplates = (Map<String, Object>) CachingCommonData.get(key);
        if (Utils.isEmpty(emailTemplates)) {
            String templateUrl = GetEmailTemplateByID + "/" + emailTemplateId;

            String loginToken = BackEndLogin(companyName);
            HttpHeaders headersRule = new HttpHeaders();
            headersRule.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headersRule.set("SessionToken", loginToken);
            headersRule.set("User-Agent", "Smart2GoWorkFlowEngine");


            HttpEntity<Map<String, Object>> entityRule = new HttpEntity<>(headersRule);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(templateUrl, HttpMethod.GET, entityRule,
                    Map.class);

            Map<String, Object> fetchedRules = responseEntity.getBody();

            if (Utils.isNotEmpty(fetchedRules)) {
                emailTemplates = fetchedRules;
                CachingCommonData.put(key, emailTemplates);
            }
        }
        return emailTemplates;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTaskRouting(Long taskId) {
        String key = "Task_" + taskId;
        List<Map<String, Object>> taskRouties = (List<Map<String, Object>>) CachingCommonData.get(key);
        if (Utils.isEmpty(taskRouties)) {
            String routesSql = "SELECT * FROM PM_TASK_ROUTING WHERE FROM_TASKID = " + taskId;
            List<Map<String, Object>> fetchedRules = sqlHelperDao.queryForList(routesSql);
            if (Utils.isNotEmpty(fetchedRules)) {
                taskRouties = fetchedRules;
                CachingCommonData.put(key, taskRouties);
            }
        }
        return taskRouties;
    }

    public String BackEndLogin(String companyName) throws InterruptedException {

        String key = "Login_" + companyName + "_" + loginSettings.getUserName();

        String userSessionToken = (String) CachingCommonData.get(key);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Smart2GoWorkFlowEngine");
        headers.set("Host", loginSettings.getHost());
        if (loginSettings.getUrl().contains("/rest/")) {
            loginSettings.setUrl(loginSettings.getUrl().replaceAll("/rest/", ""));
        }

        if (userSessionToken != null) {
            //test the session is not expire
            try {
                headers.set("SessionToken", userSessionToken);
                headers.set("User-Agent", "Smart2GoWorkFlowEngine");

                HttpEntity<String> entity = new HttpEntity<String>(null, headers);

                String TestSesisonTokenUrl = loginSettings.getUrl() + "/rest/MainFunciton/TestSesisonToken/";

                ResponseEntity<StringResponse> res = restTemplate.exchange(TestSesisonTokenUrl, HttpMethod.GET, entity,
                        StringResponse.class);
                String reomteToken = null;
                if (Utils.isNotEmpty(res.getBody())) {
                    reomteToken = res.getBody().returnValue;
                }
                if (reomteToken == null || reomteToken.contains(userSessionToken) == false) {
                    JobUtil.Debug("War:Issue in Test Session Token : " + reomteToken);
                    userSessionToken = null;//relogin again
                }
            } catch (Exception ex) {
                JobUtil.Debug("War:Issue in Test Session Token : " + ex.getMessage());
                userSessionToken = null;//relogin again
            }


        }


        while (userSessionToken == null) {
            try {
                LoginUserRequest loginUser = new LoginUserRequest();
                headers.set("SessionToken", companyName);
                headers.set("User-Agent", "Smart2GoWorkFlowEngine");

                SessionInfo sInfo = new SessionInfo();
                sInfo.loginUserName = loginSettings.getUserName();
                sInfo.companyName = companyName;

                JobUtil.Debug("Login URL : ===>" + loginSettings.getUrl() + "/rest/MainFunciton/login/ , C : " + companyName + " ,u :" + loginSettings.getUserName());


                loginUser.Password = loginSettings.getPassword();
                loginUser.LoginUserInfo = sInfo;

                HttpEntity<LoginUserRequest> entity = new HttpEntity<LoginUserRequest>(loginUser, headers);
                // Added By Mahmoud To fix loaded url

                String getMainMethodUrl = loginSettings.getUrl() + "/rest/MainFunciton/login/";
                ResponseEntity<LoginUser> res = restTemplate.exchange(getMainMethodUrl, HttpMethod.POST, entity,
                        LoginUser.class);
                if (Utils.isNotEmpty(res.getBody())) {
                    LoginUser user = res.getBody();
                    userSessionToken = user.UserSessionToken;
                    CachingCommonData.put(key, userSessionToken);
                    if (userSessionToken != null) {
                        JobUtil.Debug("User Session Tocken Is  : ===> Fetched Ok");
                    }
                }
            } catch (Exception ex) {
                JobUtil.Debug("War:Issue in Fetch Session Token : " + ex.getMessage());
                userSessionToken = null;
            }
            if (userSessionToken == null) {
                JobUtil.Debug("War:Sleep Try to get Session Token After 1000MSec");
                Thread.sleep(1000);
            }
        }
        return userSessionToken;
    }

    @SuppressWarnings({"rawtypes", "deprecation"})
    public Map loadCrmObject(Map row, Long typeIdForTask, Long object_id, String companyName) throws Exception {

        long typeId;
        long objectId = 0;
        if (row != null) {
            typeId = Long.parseLong(
                    (row.get("TYPE_ID") != null) ? row.get("TYPE_ID").toString() : row.get("typeid").toString());
            objectId = Long.parseLong(
                    (row.get("object_id") != null) ? row.get("object_id").toString() : row.get("object_Id").toString());
        } else {
            typeId = typeIdForTask;
            objectId = object_id;
        }
        String key = "Obj_" + objectId + "_" + typeId;
        Map loadedObj = null;
        HttpHeaders headersRoute = new HttpHeaders();
        headersRoute.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headersRoute.set("SessionToken", BackEndLogin(companyName));
        headersRoute.set("User-Agent", "Smart2GoWorkFlowEngine");

        List<Map<String, Object>> listToBeSent = new ArrayList<Map<String, Object>>();
        Map<String, Object> mapToBeSent = new Hashtable<String, Object>();

        if (row != null && row.get("OBJECT_ID") != null) {
            mapToBeSent.put("recid", Long.parseLong(row.get("OBJECT_ID").toString()));
            mapToBeSent.put("tableName", row.get("processTableName"));
            mapToBeSent.put("tasktypeid", row.get("tasktypeid"));
        } else {
            mapToBeSent.put("recid", (object_id != null) ? object_id : objectId);
        }

        listToBeSent.add(mapToBeSent);

        String loadObject = wf_loadObject + "/" + typeId;
        HttpEntity<Map<String, Object>> entityRule = new HttpEntity<>(listToBeSent.get(0), headersRoute);
        ResponseEntity<Map> responseEntity = restTemplate.exchange(loadObject, HttpMethod.POST, entityRule, Map.class);
        if (Utils.isNotEmpty(responseEntity.getBody())) {
            loadedObj = responseEntity.getBody();
            CachingCommonData.put(key, loadedObj);
        }

        return loadedObj;
    }

    // to resolve condition of Description and Email Template == Abdulrahman Helal
    @SuppressWarnings({"rawtypes", "unchecked"})
    public String replaceDescriptionWithValues(String description, Map obj, boolean isEmailTemplate, String companyName, String tenantSchema)
            throws Exception {
        String condition = description;
        String conditionField = null;
        String value = null;
        String[] splitCondition = description.split("\\.");

        for (String splitCond : splitCondition) {
            if (splitCond.contains("}}")) {
                conditionField = splitCond;
                conditionField = conditionField.substring(0, conditionField.indexOf("}}"));
                if (condition.contains(conditionField)) {
                    Object V = obj.get(conditionField);
                    if (V instanceof Long && conditionField.toLowerCase().indexOf("date") > -1) {
                        Date d = new Date((Long) V);
                        final String Format = "yyyy/MM/dd HH:mm";
                        final SimpleDateFormat DF = new SimpleDateFormat(Format);
                        value = DF.format(d);
                    } else {
                        value = (V == null) ? "" : V.toString();
                    }

                    if (!description.contains("uda")) {
                        condition = condition.replace("{{Object." + conditionField + "}}", value.toString());
                    } else if (description.contains("uda")) {
                        int index = description.indexOf("{{uda." + conditionField + "}}");
                        // String check = description.substring(index, index + 3);
                        if (index < 0) {
                            condition = condition.replace("{{Object." + conditionField + "}}", value.toString());
                        } else { // ======= here to handle uda value =======
                            ArrayList<Map> udasValues = (ArrayList<Map>) obj.get("udasValues");
                            if (udasValues != null && udasValues.size() > 0) {
                                for (Map udasValue : udasValues) {
                                    if (udasValue.get("udaName").equals(conditionField) || (Long.parseLong(udasValue.get("udaType").toString()) == IUDAType.FORM && (udasValue.get("udaName")+"_val").equals(conditionField))) {
                                        if (Long.parseLong(udasValue.get("udaType").toString()) != IUDAType.GRID) {

                                            if (Long.parseLong(udasValue.get("udaType").toString()) == IUDAType.FORM) {
                                                value = (udasValue.get("udaFormValueString") == null) ? "" : udasValue.get("udaFormValueString").toString();
                                            } else
                                                value = (udasValue.get("udaValue") == null) ? "" : udasValue.get("udaValue").toString();

                                        }
                                        // == here if it's email template and
                                        // grid uda ,, draw it's data in a
                                        // table===
                                        else if (Long.parseLong(udasValue.get("udaType").toString()) == IUDAType.GRID
                                                && isEmailTemplate == true) {
                                            ArrayList<Map> rowDatas = (ArrayList<Map>) udasValue.get("rowData");

                                            List<LinkedHashMap<String, Object>> orderedGridColumns = getGridColumns(Long.parseLong(udasValue.get("recId").toString()), companyName);
                                            value = drawGridTable(value, orderedGridColumns, rowDatas);

                                        }
                                        condition = condition.replace("{{uda." + conditionField + "}}",
                                                ((value == null) ? "" : value.toString()));

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return condition.replace("'", "''");
    }

    // to resolve condition of Description and Email Template == Abdulrahman Helal
    @SuppressWarnings({"rawtypes", "unchecked"})
    public SendMail replaceDescriptionWithValuesInMail(String description, Map obj, boolean isEmailTemplate, String companyName, String tenantSchema,
                                                       ToDoList row, boolean isActionableMail,Long typeId, Long objectId)
            throws Exception {
        String condition = description;
        String conditionField = null;
        String value = null;
        String[] splitCondition = description.split("\\.");

        List<Map<String, Object>> actions = null;

        if (isActionableMail && Utils.isNotEmpty(row.getUdaName()) && Utils.isNotEmpty(row.getTaskID())) {
            String query = "";
            query = "select distinct route_if,ignor_mandatory_check,company_name from PM_TASK_ROUTING r join "
                    + tenantSchema + row.getUdaName() + " t on t.taskid = from_taskID  where  t.recid = "
                    + row.getIncom_RecID() + " and r.company_name = '" + companyName + "'";
            actions = sqlHelperDao.queryForList(query);
        }

        SendMail sendMailData = new SendMail();
        int imgeNUM = 0;

        for (String splitCond : splitCondition) {
            if (splitCond.contains("}}")) {
                conditionField = splitCond;
                conditionField = conditionField.substring(0, conditionField.indexOf("}}"));
                if (condition.contains(conditionField)) {
                    if (description.contains("TaskActions") && Utils.isNotEmpty(actions)) {
                        List<SaveThreadTransactionInformation> txs = new ArrayList<SaveThreadTransactionInformation>();


                        String htmlActions = "";

                        for (int i = 0; i < actions.size(); i++) {
                            HumanTaskActions action = new HumanTaskActions();
                            String actionToken = UUID.randomUUID().toString();
                            action.setRowID(row.getRecId());
                            action.setObjectID(row.getObject_Id());
                            action.setTaskID(row.getTaskID());
                            action.setRouteIf(actions.get(i).get("route_if").toString());
                            action.setToken(actionToken);


                            htmlActions += "<a style=\"margin-right: 30px;background-color: #0078d7;color: white;padding: 0.5em 1em;text-decoration: none;font-weight: 600;border-radius: 5px;\"\n"
                                    + " href=\"" + GetServerPublicURL + "/" + action.getToken()
                                    + "\" target=\"_blank\">" + actions.get(i).get("route_if").toString() + "</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                            //humanTaskActions.add(action);
                            SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(action,
                                    SaveThreadTransactionInformation.TxOperaiotns.Add, null);
                            txs.add(tx);
                        }

                        SaveThread.AddSaveThreadTransaction(txs);

                        condition = condition.replace("{{TaskActions}}", htmlActions);
                    } else if (!description.contains("uda")) {
                        value = "" + obj.get(conditionField) + "";
                        condition = condition.replace("{{Object." + conditionField + "}}", value.toString());
                    } else if (description.contains("uda")) {
                        int index = description.indexOf("{{uda." + conditionField);
                        // String check = description.substring(index, index + 4);
                        if (index < 0) {
                            value = "" + obj.get(conditionField) + "";
                            condition = condition.replace("{{Object." + conditionField + "}}", value.toString());
                        } else { // ======= here to handle uda value =======
                            ArrayList<Map> udasValues = (ArrayList<Map>) obj.get("udasValues");
                            if (udasValues != null && !udasValues.isEmpty()) {
                                for (Map udasValue : udasValues) {
                                    if (udasValue.get("udaName").equals(conditionField) || (Long.parseLong(udasValue.
                                            get("udaType").toString()) == IUDAType.FORM && (udasValue.get("udaName")+"_val").equals(conditionField))) {
                                        if (Long.parseLong(udasValue.get(UDA_TYPE_KEY).toString()) != IUDAType.GRID
                                                && Long.parseLong(udasValue.get(UDA_TYPE_KEY).toString()) != IUDAType.GRID_CHART) {

                                            if (Long.parseLong(udasValue.get(UDA_TYPE_KEY).toString()) == IUDAType.FORM) {
                                                value = (udasValue.get("udaFormValueString") == null) ? ""
                                                        : udasValue.get("udaFormValueString").toString();
                                            } else
                                                value = (udasValue.get("udaValue") == null) ? ""
                                                        : udasValue.get("udaValue").toString();

                                        }
                                        // == here if it's email template and
                                        // grid uda , draw its data in a
                                        // table===
                                        else if ((Long.parseLong(udasValue.get(UDA_TYPE_KEY).toString()) == IUDAType.GRID ||
                                                Long.parseLong(udasValue.get(UDA_TYPE_KEY).toString()) == IUDAType.GRID_CHART)
                                                && isEmailTemplate) {
                                            ArrayList<Map> rowData;
                                            List<LinkedHashMap<String, Object>> orderedGridColumns;
                                            if (Long.parseLong(udasValue.get(UDA_TYPE_KEY).toString()) == IUDAType.GRID_CHART) {
                                                Map<String, Object> map = getGridChartRowData(Long.parseLong(udasValue.get("recId").toString()), typeId, objectId , companyName);
                                                orderedGridColumns = (List<LinkedHashMap<String, Object>>) map.get("gridColumns");
                                                rowData = (ArrayList<Map>) map.get("rowData");
                                            } else {
                                                rowData = (ArrayList<Map>) udasValue.get("rowData");
                                                orderedGridColumns =  getGridColumns(
                                                        Long.parseLong(udasValue.get("recId").toString()), companyName);
                                            }
                                            value = drawGridTable(value, orderedGridColumns, rowData);

                                        }
                                        condition = condition.replace("{{uda." + conditionField + "}}",
                                                ((value == null) ? "" : value.toString()));

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        sendMailData.setBody(condition);
        return sendMailData;
    }

    @SuppressWarnings("rawtypes")
    public String resolveDescription(Map<String, Object> row, String description, Long typeIdForTask, Long object_id, Map obj,
                                     boolean isEmailTemplate, String companyName, String tenantSchema) throws Exception {

        return replaceDescriptionWithValues(description, obj, isEmailTemplate, companyName, tenantSchema);
    }

    public List<Map<String, Object>> geturgancyList(String companyName) {
        List<Map<String, Object>> urgancy = (List<Map<String, Object>>) CachingCommonData.get("urgancy_" + companyName);
        if (urgancy == null) {

            String Dquery = "select * from adm_urgent where is_global_tenant = '1' or company_name = '" + companyName + "'";

            urgancy = sqlHelperDao.queryForList(Dquery);
            CachingCommonData.put("urgancy_" + companyName, urgancy);
        }
        return urgancy;
    }

    public String getTenantSchemaName(String companyName) {
        String tenantSchema = (Utils.isNotEmpty(CachingCommonData.get(companyName))) ? CachingCommonData.get(companyName).toString() : null;
        return tenantSchema;
    }

    public void cacheTenantSchemaName(String companyName, String tenantSchema) {
        CachingCommonData.put(companyName, tenantSchema);
    }

    //Dev-00003512: grid columns not ordered right in Email Template
    public List<LinkedHashMap<String, Object>> getGridColumns(Long udaGridRecId, String companyName) throws Exception {

        String gridColumnsUrl = GetOrderedGridColumnsURL + "/" + udaGridRecId;

        String loginToken = BackEndLogin(companyName);
        HttpHeaders headersRule = new HttpHeaders();
        headersRule.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headersRule.set("SessionToken", loginToken);
        headersRule.set("User-Agent", "Smart2GoWorkFlowEngine");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headersRule);
        ResponseEntity<Object> responseEntity = restTemplate.exchange(gridColumnsUrl, HttpMethod.GET, entity, Object.class);
        List<LinkedHashMap<String, Object>> gridColumns = (List<LinkedHashMap<String, Object>>) responseEntity.getBody();


        return gridColumns;
    }

    //Dev-00003512: grid columns not ordered right in Email Template
    public String drawGridTable(String value, List<LinkedHashMap<String, Object>> orderedGridColumns, ArrayList<Map> rowDatas) {

        if (rowDatas != null && rowDatas.size() > 0 && orderedGridColumns.size() > 0) {
            value = "</p><br/> ";
            value += "<table border='2'>";
            value += "<tr border='2'>";

            // to draw headers of table
            for (LinkedHashMap<String, Object> UdaGridColumn : orderedGridColumns) {
                value += "<th border='2' align='center'> <font size='4'> <strong>"
                        + UdaGridColumn.get("caption").toString() + "</strong></font> </th>";
            }

            value += "</tr>";
            String colVal = "";
            // to draw rows data
            for (Map rawData : rowDatas) {
                value += "<tr border='2'>";
                for (LinkedHashMap<String, Object> UdaGridColumn : orderedGridColumns) {

                    colVal = Utils.isNotEmpty(rawData.get(UdaGridColumn.get("name")))? rawData.get(UdaGridColumn.get("name")).toString() : "";

                    value += "<td border='1'> <font size='3'>"
                            + colVal + "</font></td>";
                }
                value += "</tr>";
            }
            value += "</table>";
            value += "<p>";
        }

        return value;
    }

    public byte[] loadEmailAttachments(Long emailId, Long objectId, String companyName) throws Exception {
        byte[] loadedAttachments = null;
        HttpHeaders headersRoute = new HttpHeaders();
        headersRoute.setContentType(MediaType.APPLICATION_JSON);
        headersRoute.set("SessionToken", BackEndLogin(companyName));
        headersRoute.set("User-Agent", "Smart2GoWorkFlowEngine");
        String loadObject = wf_loadAttachments  + emailId + "/" + objectId;
        HttpEntity<Map<String, Object>> entityRule = new HttpEntity<>(headersRoute);
        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(loadObject, HttpMethod.GET, entityRule, byte[].class);
        if (Utils.isNotEmpty(responseEntity.getBody())) {
            loadedAttachments = responseEntity.getBody();
        }
        return loadedAttachments;
    }

    private Map<String, Object> getGridChartRowData(long recId, Long typeId, Long objectId , String companyName) throws InterruptedException {
        Map loadedObj = null;
        HttpHeaders headersRoute = new HttpHeaders();
        headersRoute.setContentType(MediaType.APPLICATION_JSON);
        headersRoute.set("SessionToken", BackEndLogin(companyName));
        headersRoute.set("User-Agent", "Smart2GoWorkFlowEngine");

        String loadObject = wf_loadChartFields + "/" + objectId+ "/" + recId+ "/" + typeId;
        HttpEntity<Map<String, Object>> entityRule = new HttpEntity<>( headersRoute);
        ResponseEntity<Map> responseEntity = restTemplate.exchange(loadObject, HttpMethod.GET, entityRule, Map.class);
        if (Utils.isNotEmpty(responseEntity.getBody())) {
            loadedObj = responseEntity.getBody();
        }

        return loadedObj;
    }
}
