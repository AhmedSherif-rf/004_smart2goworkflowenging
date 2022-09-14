package com.ntg.engine.config;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.ntg.engine.jobs.EngineMainJob;
import com.ntg.engine.jobs.JobUtil;
import com.ntg.engine.service.JobsListener;
import com.ntg.engine.service.TriggerListner;
//import com.ntg.engine.jobs.SaveThread;

@Configuration
public class QuartzSchedulerConfig {

	/*
	 * @Autowired DataSource dataSource;
	 */

	@Autowired
	private ApplicationContext context;

	@Autowired
	private TriggerListner triggerListner;

	@Autowired
	private JobsListener jobsListener;

	@Bean
	public SchedulerFactoryBean schedulerFactoryBean() throws IOException, ParseException {

		SchedulerFactoryBean factory = new SchedulerFactoryBean();
		factory.setOverwriteExistingJobs(true);
		factory.setQuartzProperties(quartzProperties());
		factory.setGlobalTriggerListeners(triggerListner);
		factory.setGlobalJobListeners(jobsListener);
		factory.setTriggers(scheduleCronJob());
		AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
		jobFactory.setApplicationContext(context);
		factory.setJobFactory(jobFactory);
 		
		return factory;
	}

	@Bean
	public Properties quartzProperties() throws IOException {
		PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
		propertiesFactoryBean.setLocation(new ClassPathResource("/quartzWF.properties"));
		propertiesFactoryBean.afterPropertiesSet();
		return propertiesFactoryBean.getObject();
	}

	public Trigger scheduleCronJob() throws ParseException {

		String jobKey = "JobEngineMain";
		String groupKey = "NTGJobs";
		String triggerKey = "triggeEngineMain";
		JobDetail jobDetail = createJob(EngineMainJob.class, true, context, jobKey, groupKey);
		JobUtil.Info("creating trigger for key :" + jobKey + " at date :" + new Date());
		PersistableCronTriggerFactoryBean factoryBean = new PersistableCronTriggerFactoryBean();
		factoryBean.setName(triggerKey);
		factoryBean.setStartTime(new Date());
		factoryBean.setCronExpression("* * * ? * * *");
		
		factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
		factoryBean.setJobDetail(jobDetail);

		factoryBean.afterPropertiesSet();
		
		return factoryBean.getObject();

	}

	// public Trigger scheduleCronJobSaveThread() {
	//
	// // save thread job
	// String jobKey2 = "JobSaveThread";
 	// String triggerKey2 = "triggeSaveThread";
	// JobDetail jobDetail2 = createJob(SaveThread.class, true, context,
	// jobKey2, groupKey2);
 
	// PersistableCronTriggerFactoryBean factoryBean2 = new
	// PersistableCronTriggerFactoryBean();
	// factoryBean2.setName(triggerKey2);
	// factoryBean2.setStartTime(new Date());
	// factoryBean2.setCronExpression("0/10 * * * * ?");
	// factoryBean2.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
	// factoryBean2.setJobDetail(jobDetail2);
	//
	// try {
	// factoryBean2.afterPropertiesSet();
	// } catch (ParseException e) {
 	// }
	// return factoryBean2.getObject();
	//
	// }

	public JobDetail createJob(Class jobClass, boolean isDurable, ApplicationContext context, String jobName,
			String jobGroup) {
		JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
		factoryBean.setJobClass(jobClass);
		factoryBean.setDurability(isDurable);
		factoryBean.setApplicationContext(context);
		factoryBean.setName("test-job");
		factoryBean.setGroup(jobGroup);
		// set job data map
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("DS", "noNeed");
		factoryBean.setJobDataMap(jobDataMap);
		factoryBean.afterPropertiesSet();
		
		return factoryBean.getObject();
	}

}
