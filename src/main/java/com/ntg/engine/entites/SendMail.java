package com.ntg.engine.entites;

import java.util.ArrayList;
import java.util.List;

//import javax.persistence.Entity;

//@Entity
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class SendMail {

	private String sendTo;

	private String body;

	private List<String> CCList;

	private String description;

	private String subject;
	

	private ArrayList base64ImageData = new ArrayList();
	private ArrayList CIDImageData = new ArrayList();
	

	public ArrayList getBase64ImageData() {
		return base64ImageData;
	}
	public void setBase64ImageData(ArrayList base64ImageData) {
		this.base64ImageData = base64ImageData;
	}
	public ArrayList getCIDImageData() {
		return CIDImageData;
	}
	public void setCIDImageData(ArrayList cIDImageData) {
		CIDImageData = cIDImageData;
	}

	public String getSendTo() {
		//Dev-00000399 : Email Tasks to Accept mutable sender separated by semi coma
		return (sendTo==null)?"":sendTo.replaceAll(";",",");
	}

	public void setSendTo(String sendTo) {
		this.sendTo = sendTo;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public List<String> getCCList() {
		return CCList;
	}

	public void setCCList(List<String> cCList) {
		CCList = cCList;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
}
