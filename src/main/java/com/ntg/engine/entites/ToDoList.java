package com.ntg.engine.entites;

import com.fasterxml.jackson.annotation.JsonView;
import com.ntg.engine.util.View;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;

@Entity
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "sa_todo_list")
public class ToDoList implements Serializable {

    private static final long serialVersionUID = 1966608984769487061L;
    @Id
    @SequenceGenerator(allocationSize = 1, name = "sa_todo_list_s", sequenceName = "sa_todo_list_s", initialValue = 1000)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sa_todo_list_s")
    @JsonView(View.Summary.class)
    @Column(name = "recid")
    private Long recId;

    @JsonView(View.Summary.class)
    @Column(name = "processId")
    private Long processID;

    @JsonView(View.Summary.class)
    @Column(name = "assign_desc")
    public String assignDesc;

    @JsonView(View.Summary.class)
    @Column(name = "taskname")
    private String taskname;

    @JsonView(View.Summary.class)
    @Column(name = "from_dep_name")
    public String fromDepName;

    @JsonView(View.Summary.class)
    @Column(name = "to_dep_name")
    public String toDepName;

    @JsonView(View.Summary.class)
    @Column(name = "req_action")
    public String reqAction;

    @JsonView(View.Summary.class)
    @Column(name = "assign_date")
    public Timestamp AssignDate;

    @JsonView(View.Summary.class)
    @Column(name = "req_completion_date")
    public Timestamp reqCompletionDate;

    public Timestamp getExpirationDate() {
        return ExpirationDate;
    }

    public void setExpirationDate(Timestamp expirationDate) {
        ExpirationDate = expirationDate;
    }

    @JsonView(View.Summary.class)
    @Column(name = "expiration_date")
    public Timestamp ExpirationDate;





    @JsonView(View.Summary.class)
    @Column(name = "comments")
    public String Comments;

    @JsonView(View.Summary.class)
    @Column(name = "op_id")
    public Long OpId;

    @JsonView(View.Summary.class)
    @Column(name = "task_type_id")
    public Long taskTypeId;

    @JsonView(View.Summary.class)
    @Column(name = "to_employeeId")
    public Long to_EmployeeID;

    @JsonView(View.Summary.class)
    @Column(name = "to_groupId")
    public Long To_GroupID;

    @JsonView(View.Summary.class)
    @Column(name = "attached_recid")
    public Long attachedRecID;

    @JsonView(View.Summary.class)
    @Column(name = "utlized_for_this_project_from")
    public Timestamp utlizedForThisProjectFrom;

    @JsonView(View.Summary.class)
    @Column(name = "utlized_for_this_project_to")
    public Timestamp utlizedForThisProjectTo;

    @JsonView(View.Summary.class)
    @Column(name = "utlized_for_this_project")
    public Double utlizedForThisProject;// NUMBER (4) DEFAULT 0

    @JsonView(View.Summary.class)
    @Column(name = "sub_status_task_id")
    public Long subStatusTaskID;

    @JsonView(View.Summary.class)
    @Column(name = "uda_panel_caption")
    public Long TierReason1ID;
    
    @JsonView(View.Summary.class)
    @Column(name = "tierreason3")
    public String TierReason3;
    
    @JsonView(View.Summary.class)
    @Column(name = "done_date")
    public Timestamp DoneDate;

    @JsonView(View.Summary.class)
    @Column(name = "start_date")
    public Timestamp startDate;

    @JsonView(View.Summary.class)
    @Column(name = "escalation_Id")
    public Long escalationID;

    @JsonView(View.Summary.class)
    @Column(name = "priority")
    public String priority;

    @JsonView(View.Summary.class)
    @Column(name = "urgent_ID")
    public Long urgent_ID;


    @JsonView(View.Summary.class)
    @Column(name = "priority_Color")
    public String priorityColor;


    @JsonView(View.Summary.class)
    @Column(name = "total_planned_hour_effort")
    public Double totalPlannedHourEffort;

    @JsonView(View.Summary.class)
    @Column(name = "milestone_inAssign")
    public String MilestoneInAssign;

    @JsonView(View.Summary.class)
    @Column(name = "milestone_caption")
    public String MilestoneCaption;

    @JsonView(View.Summary.class)
    @Column(name = "CC_List")
    public String ccList;

    @JsonView(View.Summary.class)
    @Column(name = "started_by_Id")
    public Long startedById;

    @JsonView(View.Summary.class)
    @Column(name = "elapsed_effort")
    public Double elapsedEffort;

    @JsonView(View.Summary.class)
    @Column(name = "first_start_date")
    public Timestamp firstStartDate;

    @JsonView(View.Summary.class)
    @Column(name = "start_by_name")
    public String startbyname;

    @JsonView(View.Summary.class)
    @Column(name = "Incom_RecID")
    public Long Incom_RecID;

    @JsonView(View.Summary.class)
    @Column(name = "project_number")
    public String projectNumber;

    @JsonView(View.Summary.class)
    @Column(name = "cost_type_Id")
    public String costTypeID;

    @JsonView(View.Summary.class)
    @Column(name = "cost_type_name")
    public String costTypeName;

    @JsonView(View.Summary.class)
    @Column(name = "attached_request_data")
    public byte[] attachedRequestData;

    @JsonView(View.Summary.class)
    @Column(name = "reported_percentage_done_work")
    public Long reportedPercentageDoneWork;

    @JsonView(View.Summary.class)
    @Column(name = "object_Id")
    public Long object_Id;

    @JsonView(View.Summary.class)
    @Column(name = "uda_Id")
    public Long udaID;

    @JsonView(View.Summary.class)
    @Column(name = "udaName")
    public String udaName;

    @JsonView(View.Summary.class)
    @Column(name = "pmtaskRecid")
    public Long PMtaskRecid;

    @JsonView(View.Summary.class)
    @Column(name = "task_Id")
    public Long taskID;

    @JsonView(View.Summary.class)
    @Column(name = "typeid")
    private Long typeid;

    @JsonView(View.Summary.class)
    @Column(name = "module_Id")
    public Long module_Id;

    @JsonView(View.Summary.class)
    @Column(name = "ordered_members_images_ids")
    public String orderedMembersImagesIds;
    // =added for mail temp and mangerial level by Abdulrahman Helal
    @JsonView(View.Summary.class)
    @Column(name = "emai_template_id")
    public Long emailTemplateId;

    @JsonView(View.Summary.class)
    @Column(name = "from_mangerial_level")
    public Long fromMangerialLevel;

    @JsonView(View.Summary.class)
    @Column(name = "to_mangerial_level")
    public Long toMangerialLevel;

    @JsonView(View.Summary.class)
    @Column(name = "next_approval_if")
    public String nextApprovalIf;

    @JsonView(View.Summary.class)
    @Column(name = "dynamic_mail")
    public String dynamic_mail;

    @Column(name = "company_name", nullable = false, updatable = false)
    private String companyName;
    
    @Transient
    private String tenantSchema;

    @JsonView(View.Summary.class)
    @Column(name = "is_Add_Hock")
    @ColumnDefault("'0'")
    public Boolean isAddHock;

    @JsonView(View.Summary.class)
    @Column(name = "is_child")
    @ColumnDefault("'0'")
    public Boolean ischild;

    @JsonView(View.Summary.class)
    @Column(name = "minimum_num_of_approval")
    public Long minimumNumOfApproval;

    // to be use to retrieve images for members on task
    @JsonView(View.Summary.class)
    @Transient
    public ArrayList<String> memberImagesHelper;

    @JsonView(View.Summary.class)
    @Transient
    private String actionableEmail;

    @JsonView(View.Summary.class)
    @Column(name = "inherit_All_Rules" )
    @ColumnDefault("'0'")
    private boolean inheritAllRules;

    // =============================

    public String getDynamic_mail() {
        return dynamic_mail;
    }

    public void setDynamic_mail(String dynamic_mail) {
        this.dynamic_mail = dynamic_mail;
    }

    // for handling routing for parent task id
    @JsonView(View.Summary.class)
    @Column(name = "parent_task_id")
    public Long parentTaskId;

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    @Transient
    public boolean sendEmailonAssign = false;

    public Long getTypeid() {
        return typeid;
    }

    public void setTypeid(Long typeid) {
        this.typeid = typeid;
    }

    public Long getModuleId() {
        return module_Id;
    }

    public void setModuleId(Long moduleId) {
        this.module_Id = moduleId;
    }

    public Long getRecId() {
        return recId;
    }

    public void setRecId(Long recId) {
        this.recId = recId;
    }

    public Long getProcessID() {
        return processID;
    }

    public void setProcessID(Long processID) {
        this.processID = processID;
    }

    public String getAssignDesc() {
        return assignDesc;
    }

    public void setAssignDesc(String assignDesc) {
        this.assignDesc = assignDesc;
    }

    public String getTaskname() {
        return taskname;
    }

    public void setTaskname(String taskname) {
        this.taskname = taskname;
    }

    public String getFromDepName() {
        return fromDepName;
    }

    public void setFromDepName(String fromDepName) {
        this.fromDepName = fromDepName;
    }

    public String getToDepName() {
        return toDepName;
    }

    public void setToDepName(String toDepName) {
        this.toDepName = toDepName;
    }

    public String getReqAction() {
        return reqAction;
    }

    public void setReqAction(String reqAction) {
        this.reqAction = reqAction;
    }

    public Timestamp getAssignDate() {
        return AssignDate;
    }

    public void setAssignDate(Timestamp assignDate) {
        AssignDate = assignDate;
    }

    public Timestamp getReqCompletionDate() {
        return reqCompletionDate;
    }

    public void setReqCompletionDate(Timestamp reqCompletionDate) {
        this.reqCompletionDate = reqCompletionDate;
    }

    public String getComments() {
        return Comments;
    }

    public void setComments(String comments) {
        Comments = comments;
    }

    public Long getOpId() {
        return OpId;
    }

    public void setOpId(Long opId) {
        OpId = opId;
    }

    public Long getTo_EmployeeID() {
        return (to_EmployeeID == null) ? -1 : to_EmployeeID;
    }

    public void setTo_EmployeeID(Long to_EmployeeID) {
        this.to_EmployeeID = to_EmployeeID;
    }

    public Long getTo_GroupID() {
        return To_GroupID;
    }

    public void setTo_GroupID(Long to_GroupID) {
        To_GroupID = to_GroupID;
    }

    public Long getAttachedRecID() {
        return attachedRecID;
    }

    public void setAttachedRecID(Long attachedRecID) {
        this.attachedRecID = attachedRecID;
    }

    public Timestamp getUtlizedForThisProjectFrom() {
        return utlizedForThisProjectFrom;
    }

    public void setUtlizedForThisProjectFrom(Timestamp utlizedForThisProjectFrom) {
        this.utlizedForThisProjectFrom = utlizedForThisProjectFrom;
    }

    public Timestamp getUtlizedForThisProjectTo() {
        return utlizedForThisProjectTo;
    }

    public void setUtlizedForThisProjectTo(Timestamp utlizedForThisProjectTo) {
        this.utlizedForThisProjectTo = utlizedForThisProjectTo;
    }

    public Double getUtlizedForThisProject() {
        return utlizedForThisProject;
    }

    public void setUtlizedForThisProject(Double utlizedForThisProject) {
        this.utlizedForThisProject = utlizedForThisProject;
    }

    public Long getSubStatusTaskID() {
        return subStatusTaskID;
    }

    public void setSubStatusTaskID(Long subStatusTaskID) {
        this.subStatusTaskID = subStatusTaskID;
    }

    public Long getTierReason1ID() {
        return TierReason1ID;
    }

    public void setTierReason1ID(Long tierReason1ID) {
        TierReason1ID = tierReason1ID;
    }

    public String getTierReason3() {
        return TierReason3;
    }

    public void setTierReason3(String tierReason3) {
        TierReason3 = tierReason3;
    }

    public Timestamp getDoneDate() {
        return DoneDate;
    }

    public void setDoneDate(Timestamp doneDate) {
        DoneDate = doneDate;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Long getEscalationID() {
        return escalationID;
    }

    public void setEscalationID(Long escalationID) {
        this.escalationID = escalationID;
    }

    public String getPriority() {
        return priority;

    }

    public void setPriority(String priority) {
        this.priority = priority;

    }

    public String getPriorityColor() {
        return priorityColor;
    }

    public void setPriorityColor(String priorityColor) {
        this.priorityColor = priorityColor;
    }


    public Double getTotalPlannedHourEffort() {
        return totalPlannedHourEffort;
    }

    public void setTotalPlannedHourEffort(Double totalPlannedHourEffort) {
        this.totalPlannedHourEffort = totalPlannedHourEffort;
    }

    public String getMilestoneInAssign() {
        return MilestoneInAssign;
    }

    public void setMilestoneInAssign(String milestoneInAssign) {
        MilestoneInAssign = milestoneInAssign;
    }

    public String getMilestoneCaption() {
        return MilestoneCaption;
    }

    public void setMilestoneCaption(String milestoneCaption) {
        MilestoneCaption = milestoneCaption;
    }

    public String getCcList() {
        return ccList;
    }

    public void setCcList(String ccList) {
        this.ccList = ccList;
    }

    public Long getStartedById() {
        return startedById;
    }

    public void setStartedById(Long startedById) {
        this.startedById = startedById;
    }

    public Double getElapsedEffort() {
        return elapsedEffort;
    }

    public void setElapsedEffort(Double elapsedEffort) {
        this.elapsedEffort = elapsedEffort;
    }

    public Timestamp getFirstStartDate() {
        return firstStartDate;
    }

    public void setFirstStartDate(Timestamp firstStartDate) {
        this.firstStartDate = firstStartDate;
    }

    public String getStartbyname() {
        return startbyname;
    }

    public void setStartbyname(String startbyname) {
        this.startbyname = startbyname;
    }

    public Long getIncom_RecID() {
        return Incom_RecID;
    }

    public void setIncom_RecID(Long incom_RecID) {
        Incom_RecID = incom_RecID;
    }

    public String getProjectNumber() {
        return projectNumber;
    }

    public void setProjectNumber(String projectNumber) {
        this.projectNumber = projectNumber;
    }

    public String getCostTypeID() {
        return costTypeID;
    }

    public void setCostTypeID(String costTypeID) {
        this.costTypeID = costTypeID;
    }

    public String getCostTypeName() {
        return costTypeName;
    }

    public void setCostTypeName(String costTypeName) {
        this.costTypeName = costTypeName;
    }

    public byte[] getAttachedRequestData() {
        return attachedRequestData;
    }

    public void setAttachedRequestData(byte[] attachedRequestData) {
        this.attachedRequestData = attachedRequestData;
    }

    public Long getReportedPercentageDoneWork() {
        return reportedPercentageDoneWork;
    }

    public void setReportedPercentageDoneWork(Long reportedPercentageDoneWork) {
        this.reportedPercentageDoneWork = reportedPercentageDoneWork;
    }

    public ToDoList() {

    }

    public Long getObject_Id() {
        return object_Id;
    }

    public void setObject_Id(Long object_Id) {
        this.object_Id = object_Id;
    }

    public Long getUdaID() {
        return udaID;
    }

    public void setUdaID(Long udaID) {
        this.udaID = udaID;
    }

    public Long getTaskID() {
        return taskID;
    }

    public void setTaskID(Long taskID) {
        this.taskID = taskID;
    }

    public String getUdaName() {
        return udaName;
    }

    public void setUdaName(String udaName) {
        this.udaName = udaName;
    }

    public Long getPMtaskRecid() {
        return PMtaskRecid;
    }

    public void setPMtaskRecid(Long pMtaskRecid) {
        PMtaskRecid = pMtaskRecid;
    }

    public Long getTaskTypeId() {
        return taskTypeId;
    }

    public void setTaskTypeId(Long taskTypeId) {
        this.taskTypeId = taskTypeId;
    }

    public Long getEmailTemplateId() {
        return emailTemplateId;
    }

    public void setEmailTemplateId(Long emailTemplateId) {
        this.emailTemplateId = emailTemplateId;
    }

    public Long getFromMangerialLevel() {
        return fromMangerialLevel;
    }

    public void setFromMangerialLevel(Long fromMangerialLevel) {
        this.fromMangerialLevel = fromMangerialLevel;
    }

    public Long getToMangerialLevel() {
        return toMangerialLevel;
    }

    public void setToMangerialLevel(Long toMangerialLevel) {
        this.toMangerialLevel = toMangerialLevel;
    }

    public String getNextApprovalIf() {
        return nextApprovalIf;
    }

    public void setNextApprovalIf(String nextApprovalIf) {
        this.nextApprovalIf = nextApprovalIf;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

	public String getTenantSchema() {
		return tenantSchema;
	}

	public void setTenantSchema(String tenantSchema) {
		this.tenantSchema = tenantSchema;
	}

    public Boolean getIsAddHock() {
        return isAddHock;
    }

    public void setIsAddHock(Boolean isAddHock) {
        this.isAddHock = isAddHock;
    }

    public Boolean getIschild() {
        return ischild;
    }

    public void setIschild(Boolean ischild) {
        this.ischild = ischild;
    }

    public Long getMinimumNumOfApproval() {
        return minimumNumOfApproval;
    }

    public void setMinimumNumOfApproval(Long minimumNumOfApproval) {
        this.minimumNumOfApproval = minimumNumOfApproval;
    }

    public String getOrderedMembersImagesIds() {
        return orderedMembersImagesIds;
    }

    public void setOrderedMembersImagesIds(String orderedMembersImagesIds) {
        this.orderedMembersImagesIds = orderedMembersImagesIds;
    }

    public String getActionableEmail() {
        return actionableEmail;
    }

    public void setActionableEmail(String actionableEmail) {
        this.actionableEmail = actionableEmail;
    }

    public boolean getInheritAllRules() {
        return inheritAllRules;
    }

    public void setInheritAllRules(boolean inheritAllRules) {
        this.inheritAllRules = inheritAllRules;
    }
}
