package com.ntg.engine.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class LoginUserRequest {

	public SessionInfo LoginUserInfo;

	@JsonInclude(Include.NON_EMPTY)
	public String Password;
	@JsonInclude(Include.NON_EMPTY)
	public boolean isAdmin;
	@JsonInclude(Include.NON_EMPTY)
	public String LoginMachineName;
	@JsonInclude(Include.NON_EMPTY)
	public String LoginIPAddress;
	@JsonInclude(Include.NON_EMPTY)
	public String AppPort;

}
