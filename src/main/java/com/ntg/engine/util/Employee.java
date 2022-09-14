package com.ntg.engine.util;

public class Employee {

	public String EmployeeName;
	public long GroupID;
	public boolean IsMember;
	public long UserID;
	public String email;

	public String getEmployeeName() {
		return EmployeeName;
	}

	public void setEmployeeName(String employeeName) {
		EmployeeName = employeeName;
	}

	public long getGroupID() {
		return GroupID;
	}

	public void setGroupID(long groupID) {
		GroupID = groupID;
	}

	public boolean isIsMember() {
		return IsMember;
	}

	public void setIsMember(boolean isMember) {
		IsMember = isMember;
	}

	public long getUserID() {
		return UserID;
	}

	public void setUserID(long userID) {
		UserID = userID;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
}
