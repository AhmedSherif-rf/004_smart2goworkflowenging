package com.ntg.engine.util;

import java.sql.Time;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EmployeeInfo {

	public static char[] Enc = { 'N', 'T', 'G' };
	public String gender;
	public String Name;
	public String Email;
	public String Password;
	public String User_Name;	
	public String Name_First;
	public String Name_Last;
	public String Mobile_Phone;
 	public long Status_ID;
	public long loginType;
	public long Emp_ID;
	public String CORPORATE_ID;
	public long DeptID;
	public String locationName;
	public long PARENT_ID;
	public String statusName;
	public double HourCost;
	public long workinghours;
	public String WorkingDays;
	public Time startWorkingHour = new Time(90 * 1000 * 60 * 60);
	public Date Expire_Date;
	
	public String Prefered_Language="en";
	public long ContractTypeID=1;
	@JsonIgnore
	public byte[] SmallImage;
	
	public byte[] image;

	public boolean isRegistering;
	
	public void setStartWorkingHelper(String startWorkingHelper) {
		if(startWorkingHelper != null){
		 	LocalTime localTime = LocalTime.parse(startWorkingHelper);
			System.out.println("startWorkingHour : "+Time.valueOf(localTime));
			this.startWorkingHour = Time.valueOf(localTime);			
		}
	}

	public void setHelperImage(String helperImage) {
		if(helperImage != null){
			image = Base64.getDecoder().decode(helperImage);
			SmallImage = image;
		}
	}

	public void setHelperPassword(String helperPassword) {
		if (helperPassword == null || helperPassword.equals("")) {
			Password = ""  ;
		}
		int j = 0;
		char[] C = helperPassword.toCharArray();
		for (int i = 0; i < C.length; i++) {
			C[i] ^= Enc[j];
			j++;
			if (j > 2) {
				j = 0;
			}
		}
		System.out.println("setPassword  : "+String.valueOf(C));
		Password = String.valueOf(C);
	}
		
}