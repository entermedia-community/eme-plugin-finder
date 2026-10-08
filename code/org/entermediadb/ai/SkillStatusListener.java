package org.entermediadb.ai;

import org.entermediadb.ai.agentjobs.AgentJobStep;

public interface SkillStatusListener
{
	void handleStatusStarting(AgentContext inContext, AgentJobStep inAutomationStep);

	void handleStatusComplete(AgentContext inContext, AgentJobStep inAutomationStep);
}
