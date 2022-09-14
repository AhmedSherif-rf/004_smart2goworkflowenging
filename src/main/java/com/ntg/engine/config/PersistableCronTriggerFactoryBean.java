package com.ntg.engine.config;

import java.text.ParseException;
import java.util.Date;

import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;

/**
 * Needed to set Quartz useProperties=true when using Spring classes, because Spring sets an object reference on
 * JobDataMap that is not a String
 * 
 * @see http://site.trimplement.com/using-spring-and-quartz-with-jobstore-properties/
 * @see http://forum.springsource.org/showthread.php?130984-Quartz-error-IOException
 */
public class PersistableCronTriggerFactoryBean extends CronTriggerFactoryBean {

	private Date endTime;

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	@Override
	public void afterPropertiesSet() throws ParseException {
		super.afterPropertiesSet();

		if (super.getObject() != null) {
			CronTriggerImpl object = (CronTriggerImpl) super.getObject();
			object.setEndTime(endTime);
		}
	}

}