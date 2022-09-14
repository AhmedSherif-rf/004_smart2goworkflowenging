package com.ntg.engine.entites;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonView;
import com.ntg.engine.util.View;

@Entity
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "sa_human_task_actions")
public class HumanTaskActions implements Serializable{
	
	private static final long serialVersionUID = 1966608984769487069L;
	
	
	@Id
    @SequenceGenerator(allocationSize = 1, name = "sa_human_task_actions_s", sequenceName = "sa_human_task_actions_s", initialValue = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sa_human_task_actions_s")
    @JsonView(View.Summary.class)
    @Column(name = "recid")
    private Long recId;

	@JsonView(View.Summary.class)
    @Column(name = "task_id")
    private Long taskID;
	
	@JsonView(View.Summary.class)
    @Column(name = "row_id")
    private Long rowID;

    @JsonView(View.Summary.class)
    @Column(name = "object_id")
    private Long objectID;
    
    @JsonView(View.Summary.class)
    @Column(name = "route_if")
    private String routeIf;
    
    @JsonView(View.Summary.class)
    @Column(name = "token")
    private String token;
    

	public Long getRecId() {
		return recId;
	}

	public void setRecId(Long recId) {
		this.recId = recId;
	}

	public Long getTaskID() {
		return taskID;
	}

	public void setTaskID(Long taskID) {
		this.taskID = taskID;
	}

	public Long getRowID() {
		return rowID;
	}

	public void setRowID(Long rowID) {
		this.rowID = rowID;
	}

	public Long getObjectID() {
		return objectID;
	}

	public void setObjectID(Long objectID) {
		this.objectID = objectID;
	}

	public String getRouteIf() {
		return routeIf;
	}

	public void setRouteIf(String routeIf) {
		this.routeIf = routeIf;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	

}
