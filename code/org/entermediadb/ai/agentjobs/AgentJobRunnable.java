package org.entermediadb.ai.agentjobs;

import org.entermediadb.ai.AgentContext;
import org.openedit.Data;

public class AgentJobRunnable implements Runnable
{

	AgentJob fieldAgentJob;
	AgentContext fieldContext;

	public AgentContext getContext() {
		return fieldContext;
	}
	public void setContext(AgentContext inContext) {
		fieldContext = inContext;
	}

	public AgentJob getAgentJob()
	{
		return fieldAgentJob;
	}

	public void setAgentJob(AgentJob fieldJobData) 
	{
		this.fieldAgentJob = fieldJobData;
	}

	Data fieldAgentJobRun;

	public Data getAgentJobRun()
	{
		return fieldAgentJobRun;
	}

	public void setAgentJobRun(Data inAgentJobRun)
	{
		fieldAgentJobRun = inAgentJobRun;
	}

	public String getId()
	{
		return getAgentJob().getId();
	}
	protected boolean fieldCompleted;
	protected AgentJobOrchestrator fieldAgentJobOrchestrator;

	public AgentJobOrchestrator getAgentJobOrchestrator()
	{
		return fieldAgentJobOrchestrator;
	}

	public void setAgentJobOrchestrator(AgentJobOrchestrator inAgentJobOrchestrator)
	{
		fieldAgentJobOrchestrator = inAgentJobOrchestrator;
	}

	public boolean hasComplete()
	{
		return fieldCompleted;
	}

	public void setCompleted(boolean inCompleted)
	{
		fieldCompleted = inCompleted;
	}

	public AgentJobRunnable() {}

	public void run()
	{
		getAgentJobOrchestrator().startJob(this);
	}

}
