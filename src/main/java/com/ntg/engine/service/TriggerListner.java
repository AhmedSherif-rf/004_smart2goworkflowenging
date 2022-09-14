package com.ntg.engine.service;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;
import org.springframework.stereotype.Component;

@Component
public class TriggerListner implements TriggerListener {

	@Override
	public String getName() {
		return "globalTrigger";
	}

	@Override
	public void triggerFired(Trigger trigger, JobExecutionContext context) {
		// com.ntg.engine.jobs.JobUtil.Info("TriggerListner.triggerFired()");
	}

	@Override
	public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
		// com.ntg.engine.jobs.JobUtil.Info("TriggerListner.vetoJobExecution()");
		return false;
	}

	@Override
	public void triggerMisfired(Trigger trigger) {
		// Scom.ntg.engine.jobs.JobUtil.Info("TriggerListner.triggerMisfired()");
		String jobName = trigger.getJobKey().getName();
		//com.ntg.engine.jobs.JobUtil.Info("Job name: " + jobName + " is misfired");

	}

	@Override
	public void triggerComplete(Trigger trigger, JobExecutionContext context, CompletedExecutionInstruction triggerInstructionCode) {
		// com.ntg.engine.jobs.JobUtil.Info("TriggerListner.triggerComplete()");
	}
}
