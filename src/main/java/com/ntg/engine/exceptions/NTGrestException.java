package com.ntg.engine.exceptions;

public class NTGrestException extends RuntimeException{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public NTGrestException (){
		
	}
	
	public NTGrestException (String ErrorCode , String ErrorMessage){
		
		this.ErrorCode = ErrorCode;
		this.ErrorMessage = ErrorMessage;
	}
	
	 
	public String ErrorCode = "000";
	 
	public String ErrorMessage;
	 
	public String ErrorTrace;

	 
	public String ErrorID;


}
