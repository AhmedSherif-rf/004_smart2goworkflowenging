package com.ntg.engine.exceptions;

public class NTGException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String key;
	private String message;

	public NTGException() {
 	}

	public NTGException(String key, String message) {
 		super(message);
		this.key = key;
		this.message = message;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
