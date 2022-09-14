package com.ntg.engine.util;

public class EmailTemplate {

	private long recId;
	private String name;
	private String description;
	private String templateBody;
	private String toEmail;
	private String ccEmail;
	private String emailSubject;
	private Long typeId;
	private Boolean sendContentAsAttachment;
	private String noteForAttachmentContent;

	public long getRecId() {
		return recId;
	}

	public void setRecId(long recId) {
		this.recId = recId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTemplateBody() {
		return templateBody;
	}

	public void setTemplateBody(String templateBody) {
		this.templateBody = templateBody;
	}

	public String getToEmail() {
		return (toEmail==null)?"":toEmail.replaceAll(";",",");
	}

	public void setToEmail(String toEmail) {
		this.toEmail = toEmail;
	}

	public String getCcEmail() {
		//Dev-00000399 : Email Tasks to Accept mutable sender separated by semi coma
		return (ccEmail==null)?"":ccEmail.replaceAll(";",",");
	}

	public void setCcEmail(String ccEmail) {
		this.ccEmail = ccEmail;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public Long getTypeId() {
		return typeId;
	}

	public void setTypeId(Long typeId) {
		this.typeId = typeId;
	}

	public Boolean getSendContentAsAttachment() {
		return sendContentAsAttachment;
	}

	public void setSendContentAsAttachment(Boolean sendContentAsAttachment) {
		this.sendContentAsAttachment = sendContentAsAttachment;
	}

	public String getNoteForAttachmentContent() {
		return noteForAttachmentContent;
	}

	public void setNoteForAttachmentContent(String noteForAttachmentContent) {
		this.noteForAttachmentContent = noteForAttachmentContent;
	}

}
