package com.ntg.engine.entites;

import com.ntg.engine.exceptions.NTGrestException;

/**
 * BaseResponse Will be extend by any response to have common response fields
 *
 * @author mashour@ntgclarity.com
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)

public class BaseResponse {
	
	private String code;
	private String key;
	private String message;
	private String stackTrace;

	private NTGrestException restException;

	public BaseResponse() {
		super();
	}

	public BaseResponse(String code, String key, String message) {
		super();
		this.code = code;
		this.key = key;
		this.message = message;
	}

	public BaseResponse(String code, String key, String message, String stackTrace) {
		super();
		this.code = code;
		this.key = key;
		this.message = message;
		this.stackTrace = stackTrace;
	}

	public BaseResponse(String code, String key, String message, String stackTrace, NTGrestException restException) {
		super();
		this.code = code;
		this.key = key;
		this.message = message;
		this.stackTrace = stackTrace;
		this.restException = restException;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	public NTGrestException getRestException() {
		return restException;
	}

	public void setRestException(NTGrestException restException) {
		this.restException = restException;
	}

}
