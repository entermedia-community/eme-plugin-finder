package org.entermediadb.ai.agentjobs;


/**
 * Runs an agentjob. AgentJobManager finds the jobs and picks the orchestrator bean named by the
 * agentjob's "orchestrator" field (see the chosenorchestrator list), then AgentJobRunnable calls startJob.
 */
public interface AgentJobOrchestrator
{
	/** Runs every step of the job then finishes it */
	public void startJob(AgentJobRunnable inAgentJob);

	public void runStep(AgentJobRunnable inAgentJob, AgentJobStep inStep);

	public void finishedStep(AgentJobRunnable inAgentJob, AgentJobStep inStep);

	public void finishedAllSteps(AgentJobRunnable inAgentJob);

	/** Called once the job exits, whether or not every step succeeded */
	public void finishedRun(AgentJobRunnable inAgentJob);
}
