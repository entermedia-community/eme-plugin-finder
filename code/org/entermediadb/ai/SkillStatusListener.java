package org.entermediadb.ai;

import org.entermediadb.ai.agentjobs.AgentJobStep;

public interface SkillStatusListener
{
	void handleStatusStarting(AgentContext inContext, AgentJobStep inAutomationStep);

	/** Saves and broadcasts the step's last response, the step keeps running */
	void handleStatusUpdate(AgentContext inContext, AgentJobStep inAutomationStep);

	/** Sends the update, then runs the response's exec step */
	void handleStatusComplete(AgentContext inContext, AgentJobStep inAutomationStep);
}
