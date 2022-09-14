package com.ntg.engine.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class LoginUser {
	public long ServerTimeZoneOffset;
	public String employeeId;
	@JsonInclude(Include.NON_EMPTY)
	public Boolean isHaveAdminPrev;
	public String UserSessionToken;



}
