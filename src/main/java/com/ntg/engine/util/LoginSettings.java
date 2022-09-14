package com.ntg.engine.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
//@ConfigurationProperties("loginSettings")
public class LoginSettings {

	public LoginSettings(){
		System.out.println("Password -->" + Password);
	}

	@Value("${loginSettings.systemUserName}")
	private String UserName;

	@Value("${loginSettings.password}")
	private String Password;

	@Value("${loginSettings.LoginDefualtCompanyName}")
	private String CompanyName;

	@Value("${loginSettings.url}")
	private String Url;

	@Value("${loginSettings.host}")
	private String Host;

	public String getUserName() {
		return UserName;
	}

	public void setUserName(String userName) {
		UserName = userName;
	}

	public String getPassword() {
		return Password;
	}

	public void setPassword(String password) {
		Password = password;
	}

	public String getCompanyName() {
		return CompanyName;
	}

	public void setCompanyName(String companyName) {
		CompanyName = companyName;
	}

	public String getUrl() {
		return Url;
	}

	public void setUrl(String url) {
		Url = url;
	}

	public String getHost() {
		return Host;
	}

	public void setHost(String host) {
		Host = host;
	}

	

	
}
