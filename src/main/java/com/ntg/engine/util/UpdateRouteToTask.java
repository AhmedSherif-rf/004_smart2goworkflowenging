package com.ntg.engine.util;

import java.util.ArrayList;

public class UpdateRouteToTask {
	private ArrayList<Object> param = new ArrayList<>();
	private String routeToTask;
	private Exception exception;
	private String routeLabel;

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public ArrayList<Object> getParam() {
		return param;
	}

	public void setParam(ArrayList<Object> param) {
		this.param = param;
	}

	public String getRouteToTask() {
		return routeToTask;
	}

	public void setRouteToTask(String routeToTask) {
		this.routeToTask = routeToTask;
	}

	public String getRouteLabel() {
		return routeLabel;
	}

	public void setRouteLabel(String routeLabel) {
		this.routeLabel = routeLabel;
	}

}
