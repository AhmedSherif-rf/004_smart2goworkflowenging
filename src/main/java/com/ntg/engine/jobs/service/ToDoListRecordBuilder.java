package com.ntg.engine.jobs.service;

import com.ntg.engine.entites.ToDoList;
import com.ntg.engine.repository.custom.SqlHelperDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//Dev-00002993 : Work Flow Duplicate the tasks after out of memory , need duplicate check in insert tasks
//so handle the insertion sql manually to check duplicate
public class ToDoListRecordBuilder {

    public ToDoList toDo = new ToDoList();


    ///////////////////////////////////////////


    public void setModuleId(Long moduleId) {
        toDo.setModuleId(moduleId);
        if (moduleId != null && moduleId > 0) {
            this.values.put("module_Id", moduleId);
        }
    }

    public void setTypeid(Long type_id) {
        toDo.setTypeid(type_id);
        if (type_id != null && type_id > 0) {
            this.values.put("typeid", type_id);
        }
    }

    public void setRecId(Long recId) {
        toDo.setRecId(recId);
    }

    public void setStartDate(Timestamp actualstartdate) {
        toDo.setStartDate(actualstartdate);
        if (actualstartdate != null) {
            this.values.put("start_date", actualstartdate);
        }
    }

    public void setAssignDesc(String assignDesc) {
        toDo.setAssignDesc(assignDesc);
        if (assignDesc != null) {
            this.values.put("assign_desc", assignDesc);
        }
    }

    public void setIncom_RecID(Long Incom_RecID) {
        toDo.setIncom_RecID(Incom_RecID);
        if (Incom_RecID != null && Incom_RecID > 0) {
            this.values.put("Incom_RecID", Incom_RecID);
        }
    }

    public void setTaskTypeId(long TaskTypeId) {
        toDo.setTaskTypeId(TaskTypeId);
        this.values.put("task_type_id", TaskTypeId);

    }

    public void setTaskname(String taskname) {
        toDo.setTaskname(taskname);
        if (taskname != null) {
            this.values.put("taskname", taskname);
        }
    }

    public void setPriority(String priority) {
        toDo.setPriority(priority);
        if (priority != null) {
            this.values.put("priority", priority);
        }
    }

    public void setObject_Id(Long object_id) {
        toDo.setObject_Id(object_id);
        if (object_id != null && object_id > 0) {
            this.values.put("object_Id", object_id);
        }
    }

    public void setUdaID(Long UdaID) {
        toDo.setUdaID(UdaID);
        if (UdaID != null && UdaID > 0) {
            this.values.put("uda_Id", UdaID);
        }
    }

    public void setFirstStartDate(Timestamp actualstartdate) {
        toDo.setFirstStartDate(actualstartdate);
        if (actualstartdate != null) {
            this.values.put("first_start_date", actualstartdate);
        }
    }

    public void setAssignDate(Timestamp assign_date) {
        toDo.setAssignDate(assign_date);
        if (assign_date != null) {
            this.values.put("assign_date", assign_date);
        }
    }

    public void setSubStatusTaskID(Long sub_status_task_id) {
        toDo.setSubStatusTaskID(sub_status_task_id);
        if (sub_status_task_id != null && sub_status_task_id > 0) {
            this.values.put("sub_status_task_id", sub_status_task_id);
        }
    }

    public void setUdaName(String uda_Name) {
        toDo.setUdaName(uda_Name);
        if (uda_Name != null) {
            this.values.put("uda_Name", uda_Name);
        }
    }

    public void setTaskID(Long taskid) {

        toDo.setTaskID(taskid);
        if (taskid != null && taskid > 0) {
            this.values.put("task_Id", taskid);
        }

    }

    public void setTo_EmployeeID(Long to_employeeId) {
        toDo.setTo_EmployeeID(to_employeeId);
        if (to_employeeId != null && to_employeeId > 0) {
            this.values.put("to_employee_Id", to_employeeId);
        }
    }

    public void setToDepName(String to_dep_name) {
        toDo.setToDepName(to_dep_name);
        if (to_dep_name != null) {
            this.values.put("to_dep_name", to_dep_name);
        }
    }

    public void setTo_GroupID(Long to_groupId) {
        toDo.setTo_GroupID(to_groupId);
        if (to_groupId != null && to_groupId > 0) {
            this.values.put("to_group_Id", to_groupId);
        }
    }

    public void setCcList(String CC_List) {
        toDo.setCcList(CC_List);
        if (CC_List != null) {
            this.values.put("CC_List", CC_List);
        }
    }

    public void setReqCompletionDate(Timestamp req_completion_date) {
        toDo.setReqCompletionDate(req_completion_date);
        if (req_completion_date != null) {
            this.values.put("req_completion_date", req_completion_date);
        }
    }

    public void setFromMangerialLevel(Long from_mangerial_level) {
        toDo.setFromMangerialLevel(from_mangerial_level);
        if (from_mangerial_level != null && from_mangerial_level > 0) {
            this.values.put("from_mangerial_level", from_mangerial_level);
        }
    }

    public void setToMangerialLevel(Long to_mangerial_level) {
        toDo.setToMangerialLevel(to_mangerial_level);
        if (to_mangerial_level != null && to_mangerial_level > 0) {
            this.values.put("to_mangerial_level", to_mangerial_level);
        }
    }

    public void setNextApprovalIf(String next_approval_if) {
        toDo.setNextApprovalIf(next_approval_if);
        if (next_approval_if != null) {
            this.values.put("next_approval_if", next_approval_if);
        }
    }

    public void setExpirationDate(Timestamp expiration_date) {
        toDo.setExpirationDate(expiration_date);
        if (expiration_date != null) {
            this.values.put("expiration_date", expiration_date);
        }
    }

    public void setPMtaskRecid(Long pmtaskRecid) {
        toDo.setPMtaskRecid(pmtaskRecid);
        if (pmtaskRecid != null && pmtaskRecid > 0) {
            this.values.put("pmtask_Recid", pmtaskRecid);
        }
    }

    public void setParentTaskId(long parent_task_id) {
        toDo.setParentTaskId(parent_task_id);
        this.values.put("parent_task_id", parent_task_id);
    }

    public void setDynamic_mail(String dynamic_mail) {

        toDo.setDynamic_mail(dynamic_mail);
        if (dynamic_mail != null) {
            this.values.put("dynamic_mail", dynamic_mail);
        }

    }

    public void setorderedMembersImagesIds(String ordered_members_images_ids) {

        toDo.orderedMembersImagesIds = ordered_members_images_ids;
        if (ordered_members_images_ids != null) {
            this.values.put("ordered_members_images_ids", ordered_members_images_ids);
        }

    }

    public void setCompanyName(String company_name) {

        toDo.setCompanyName(company_name);
        if (company_name != null) {
            this.values.put("company_name", company_name);
        }

    }

    public void setTenantSchema(String tenantSchema) {
        toDo.setTenantSchema(tenantSchema);

    }

    public void setOpId(long op_id) {
        toDo.setOpId(op_id);
        this.values.put("op_id", op_id);

    }

    public void seturgent_ID(Long urgent_ID) {

        toDo.urgent_ID = urgent_ID;
        if (urgent_ID != null && urgent_ID > 0) {
            this.values.put("urgent_ID", urgent_ID);
        }
    }

    public void setPriorityColor(String priority_Color) {
        toDo.setPriorityColor(priority_Color);
        if (priority_Color != null) {
            this.values.put("priority_Color", priority_Color);
        }
    }

    public void setEmailTemplateId(Long emai_template_id) {

        toDo.setEmailTemplateId(emai_template_id);
        if (emai_template_id != null && emai_template_id > 0) {
            this.values.put("emai_template_id", emai_template_id);
        }
    }

    //////////////////////////

    private StringBuffer InsertSql = new StringBuffer("insert into SA_TODO_LIST(recid");
    public List<Object> pareameters = new ArrayList<>();
    private HashMap<String, Object> values = new HashMap<>();
    //////////

    private String GenerateInsertSql(long Recid) {
        Object[] keys = this.values.keySet().toArray();
        pareameters.add(Recid);
        StringBuffer fieldsStrs = new StringBuffer("?");
        for (int i = 0, n = keys.length; i < n; i++) {
            InsertSql.append(",");
            InsertSql.append(keys[i]);
            fieldsStrs.append(",");
            fieldsStrs.append("?");
            pareameters.add(this.values.get(keys[i]));
        }
        InsertSql.append(")Select ");
        InsertSql.append(fieldsStrs);
        InsertSql.append(" From dual Where Not Exists(Select 1 from SA_TODO_LIST Where Incom_RecID=? And object_Id=? And uda_Name=? And op_id = ? And task_id");
        pareameters.add(this.values.get("Incom_RecID"));
        pareameters.add(this.values.get("object_Id"));
        pareameters.add(this.values.get("uda_Name"));
        pareameters.add(this.values.get("op_id"));

        if (this.values.get("task_Id") != null) {
            InsertSql.append(" =? )");
            pareameters.add(this.values.get("task_Id"));
        } else {
            InsertSql.append(" is null )");
        }


        return InsertSql.toString();
    }

    private void clean() {
        this.values.clear();
        this.pareameters.clear();
        this.values = null;
        this.InsertSql = null;
        this.pareameters = null;
    }

    public void save(SqlHelperDao sqlHelperDao, JdbcTemplate jdbcTemplate) {

        //fetch RecID
        long Recid = sqlHelperDao.FtechRecID("SA_TODO_LIST_S");

        String sql = this.GenerateInsertSql(Recid);
        jdbcTemplate.update(sql, this.pareameters.toArray());

        this.toDo.setRecId(Recid);
        this.clean();
    }
    public void setActionableEmail(String actionableEmail) {
        toDo.setActionableEmail(actionableEmail);
    }

    public void setInheritAllRules(boolean inheritAllRules) {
        toDo.setInheritAllRules(inheritAllRules);
        this.values.put("inherit_All_Rules", inheritAllRules);
    }
}
