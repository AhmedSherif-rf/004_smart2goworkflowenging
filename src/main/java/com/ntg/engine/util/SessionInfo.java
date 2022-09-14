package com.ntg.engine.util;

import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonIgnore;

 
public class SessionInfo {

	public String loginUserName;

	public String sessionID;
	public String companyName;
	public String transactionID;
	public String userID;
	public String companyId;
	public String groupCompanyName;

	
	
 	public DataObject [] groupList;
	
	@JsonIgnore
	public DataObject [] organizationList;
	
	@JsonIgnore
	public DataObject [] companyList;

	@XmlElement(nillable = true, required = false)
	public RestASyncCallInformation restASyncCallInformation;

}
