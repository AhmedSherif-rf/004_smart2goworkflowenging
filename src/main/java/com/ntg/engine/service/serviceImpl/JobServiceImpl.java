package com.ntg.engine.service.serviceImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import com.ntg.common.NTGMessageOperation;
import com.ntg.engine.service.JobService;
import com.ntg.engine.util.JobUtil;

@Service
public class JobServiceImpl implements JobService {

	@Autowired
	@Lazy
	SchedulerFactoryBean schedulerFactoryBean;

	@Autowired
	private ApplicationContext context;

	/**
	 * Schedule a job by jobName at given date.
	 */
	@Override
	public boolean scheduleOneTimeJob(String jobName, Class<? extends QuartzJobBean> jobClass, Date date, JobDataMap jobData) {
 
		String jobKey = jobName;
		String groupKey = "WFEngineJobs";
		String triggerKey = jobName;

		JobDetail jobDetail = JobUtil.createJob(jobClass, false, context, jobKey, groupKey ,  jobData);

 		Trigger cronTriggerBean = JobUtil.createSingleTrigger(triggerKey, date, SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
		// Trigger cronTriggerBean = JobUtil.createSingleTrigger(triggerKey, date,
		// SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);

		try {
			Scheduler scheduler = schedulerFactoryBean.getScheduler();
			Date dt = scheduler.scheduleJob(jobDetail, cronTriggerBean);
 			return true;
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while scheduling job with key :" + jobKey + " message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
		}

		return false;
	}

	/**
	 * Schedule a job by jobName at given date.
	 */
	@Override
	public boolean scheduleCronJob(String jobName, Class jobClass, Date date, String cronExpression , JobDataMap jobData) {
 
		String jobKey = jobName;
		String groupKey = "WFEngineJobs";
		String triggerKey = "trigger" + jobName;

		JobDetail jobDetail = JobUtil.createJob(jobClass, false, context, jobKey, groupKey ,jobData);

 		Trigger cronTriggerBean = JobUtil.createCronTrigger(triggerKey, date, cronExpression, SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

		try {
			Scheduler scheduler = schedulerFactoryBean.getScheduler();
			Date dt = scheduler.scheduleJob(jobDetail, cronTriggerBean);
 			return true;
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while scheduling job with key :" + jobKey + " message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
		}

		return false;
	}

	/**
	 * Update one time scheduled job.
	 */
	@Override
	public boolean updateOneTimeJob(String jobName, Date date) {
 
		String jobKey = jobName;

 		try {
			// Trigger newTrigger = JobUtil.createSingleTrigger(jobKey, date,
			// SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
			Trigger newTrigger = JobUtil.createSingleTrigger(jobKey, date, SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

			Date dt = schedulerFactoryBean.getScheduler().rescheduleJob(TriggerKey.triggerKey(jobKey), newTrigger);
 			return true;
		} catch (Exception e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while updating one time job with key :" + jobKey + " message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
			return false;
		}
	}

	/**
	 * Update scheduled cron job.
	 */
	@Override
	public boolean updateCronJob(String jobName, Date date, String cronExpression) {
 
		String jobKey = jobName;

 		try {
			// Trigger newTrigger = JobUtil.createSingleTrigger(jobKey, date,
			// SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
			Trigger newTrigger = JobUtil.createCronTrigger(jobKey, date, cronExpression, SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

			Date dt = schedulerFactoryBean.getScheduler().rescheduleJob(TriggerKey.triggerKey(jobKey), newTrigger);
 			return true;
		} catch (Exception e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while updating cron job with key :" + jobKey + " message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
			return false;
		}
	}

	/**
	 * Remove the indicated Trigger from the scheduler. If the related job does not have any other triggers, and the job
	 * is not durable, then the job will also be deleted.
	 */
	@Override
	public boolean unScheduleJob(String jobName) {
 
		String jobKey = jobName;

		TriggerKey tkey = new TriggerKey(jobKey);
 		try {
			boolean status = schedulerFactoryBean.getScheduler().unscheduleJob(tkey);
 			return status;
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while unscheduling job with key :" + jobKey + " message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
			return false;
		}
	}

	/**
	 * Delete the identified Job from the Scheduler - and any associated Triggers.
	 */
	@Override
	public boolean deleteJob(String jobName) {
 
		String jobKey = jobName;
		String groupKey = "WFEngineJobs";

		JobKey jkey = new JobKey(jobKey, groupKey);
 
		try {
			boolean status = schedulerFactoryBean.getScheduler().deleteJob(jkey);
 			return status;
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while deleting job with key :" + jobKey + " message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
			return false;
		}
	}

	/**
	 * Pause a job
	 */
	@Override
	public boolean pauseJob(String jobName) {
 
		String jobKey = jobName;
		String groupKey = "WFEngineJobs";
		JobKey jkey = new JobKey(jobKey, groupKey);
 
		try {
			schedulerFactoryBean.getScheduler().pauseJob(jkey);
 			return true;
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while pausing job with key :" + jobName + " message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
return false;
		}
	}

	/**
	 * Resume paused job
	 */
	@Override
	public boolean resumeJob(String jobName) {
 
		String jobKey = jobName;
		String groupKey = "WFEngineJobs";

		JobKey jKey = new JobKey(jobKey, groupKey);
 		try {
			schedulerFactoryBean.getScheduler().resumeJob(jKey);
 			return true;
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while resuming job with key :" + jobKey + " message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
			return false;
		}
	}

	/**
	 * Start a job now
	 */
	@Override
	public boolean startJobNow(String jobName) {
 
		String jobKey = jobName;
		String groupKey = "WFEngineJobs";

		JobKey jKey = new JobKey(jobKey, groupKey);
 		try {
			schedulerFactoryBean.getScheduler().triggerJob(jKey);
 			return true;
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while starting job now with key :" + jobKey + " message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
			return false;
		}
	}

	/**
	 * Check if job is already running
	 */
	@Override
	public boolean isJobRunning(String jobName) {
 
		String jobKey = jobName;
		String groupKey = "WFEngineJobs";

 		try {

			List<JobExecutionContext> currentJobs = schedulerFactoryBean.getScheduler().getCurrentlyExecutingJobs();
			if (currentJobs != null) {
				for (JobExecutionContext jobCtx : currentJobs) {
					String jobNameDB = jobCtx.getJobDetail().getKey().getName();
 					String groupNameDB = jobCtx.getJobDetail().getKey().getGroup();
					if (jobKey.equalsIgnoreCase(jobNameDB) && groupKey.equalsIgnoreCase(groupNameDB)) {
						return true;
					}
				}
			}
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while checking job with key :" + jobKey + " is running. error message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
			return false;
		}
		return false;
	}

	/**
	 * Get all jobs
	 */
	@Override
	public List<Map<String, Object>> getAllJobs() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {
			Scheduler scheduler = schedulerFactoryBean.getScheduler();

			for (String groupName : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

					String jobName = jobKey.getName();
					String jobGroup = jobKey.getGroup();

					// get job's trigger
					List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
					Date scheduleTime = triggers.get(0).getStartTime();
					Date nextFireTime = triggers.get(0).getNextFireTime();
					Date lastFiredTime = triggers.get(0).getPreviousFireTime();

					Map<String, Object> map = new HashMap<String, Object>();
					map.put("jobName", jobName);
					map.put("groupName", jobGroup);
					map.put("scheduleTime", scheduleTime);
					map.put("lastFiredTime", lastFiredTime);
					map.put("nextFireTime", nextFireTime);

					if (isJobRunning(jobName)) {
						map.put("jobStatus", "RUNNING");
					} else {
						String jobState = getJobState(jobName);
						map.put("jobStatus", jobState);
					}

					/*
					 * Date currentDate = new Date(); if (scheduleTime.compareTo(currentDate) > 0) {
					 * map.put("jobStatus", "scheduled"); } else if (scheduleTime.compareTo(currentDate) < 0) {
					 * map.put("jobStatus", "Running"); } else if (scheduleTime.compareTo(currentDate) == 0) {
					 * map.put("jobStatus", "Running"); }
					 */

					list.add(map);
	 			}

			}
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while fetching all jobs. error message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
		}
		return list;
	}

	/**
	 * Check job exist with given name
	 */
	@Override
	public boolean isJobWithNamePresent(String jobName) {
		try {
			String groupKey = "WFEngineJobs";
			JobKey jobKey = new JobKey(jobName, groupKey);
			Scheduler scheduler = schedulerFactoryBean.getScheduler();
			if (scheduler.checkExists(jobKey)) {
				return true;
			}
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while checking job with name and group exist:" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
		}
		return false;
	}

	/**
	 * Get the current state of job
	 */
	public String getJobState(String jobName) {
 
		try {
			String groupKey = "WFEngineJobs";
			JobKey jobKey = new JobKey(jobName, groupKey);

			Scheduler scheduler = schedulerFactoryBean.getScheduler();
			JobDetail jobDetail = scheduler.getJobDetail(jobKey);

			List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());
			if (triggers != null && triggers.size() > 0) {
				for (Trigger trigger : triggers) {
					TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());

					if (TriggerState.PAUSED.equals(triggerState)) {
						return "PAUSED";
					} else if (TriggerState.BLOCKED.equals(triggerState)) {
						return "BLOCKED";
					} else if (TriggerState.COMPLETE.equals(triggerState)) {
						return "COMPLETE";
					} else if (TriggerState.ERROR.equals(triggerState)) {
						return "ERROR";
					} else if (TriggerState.NONE.equals(triggerState)) {
						return "NONE";
					} else if (TriggerState.NORMAL.equals(triggerState)) {
						return "SCHEDULED";
					}
				}
			}
		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while checking job with name and group exist:" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
		}
		return null;
	}

	/**
	 * Stop a job
	 */
	@Override
	public boolean stopJob(String jobName) {
 		try {
			String jobKey = jobName;
			String groupKey = "WFEngineJobs";

			Scheduler scheduler = schedulerFactoryBean.getScheduler();
			JobKey jkey = new JobKey(jobKey, groupKey);

			return scheduler.interrupt(jkey);

		} catch (SchedulerException e) {
			com.ntg.engine.jobs.JobUtil.Info("SchedulerException while stopping job. error message :" + e.getMessage());
			NTGMessageOperation.PrintErrorTrace(e);
		}
		return false;
	}
}
