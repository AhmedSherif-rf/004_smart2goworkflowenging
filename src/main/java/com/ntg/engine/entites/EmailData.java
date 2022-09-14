package com.ntg.engine.entites;

import java.sql.Timestamp;
import java.util.List;

public class EmailData {
	
	public EmailData(){}
	
	private String email ;
	private List<String> ccList ;
	private String taskName ; 
	private String taskDescription ;
	private Timestamp dueDate;
	private ToDoList toDoObject ;
	
	
	
	public ToDoList getToDoObject() {
		return toDoObject;
	}
	public void setToDoObject(ToDoList toDoObject) {
		this.toDoObject = toDoObject;
	}
	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public String getTaskDescription() {
		return taskDescription;
	}
	public void setTaskDescription(String taskDescription) {
		this.taskDescription = taskDescription;
	}
	public Timestamp getDueDate() {
		return dueDate;
	}
	public void setDueDate(Timestamp dueDate) {
		this.dueDate = dueDate;
	}
	public String getEmail() {
		//Dev-00000399 : Email Tasks to Accept mutable sender separated by semi coma
		return (email==null)?"":email.replaceAll(";",",");
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public List<String> getCcList() {
		return ccList;
	}
	public void setCcList(List<String> ccList) {
		this.ccList = ccList;
	}
	
	

}
