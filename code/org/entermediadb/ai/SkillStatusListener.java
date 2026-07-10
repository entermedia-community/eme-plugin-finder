package org.entermediadb.ai;

import org.entermediadb.ai.llm.AgentEnabled;

public interface SkillStatusListener
{
	void handleStatusStarting(AgentContext inContext, AgentEnabled inAgentEnabled);

	void handleStatusComplete(AgentContext inContext, AgentEnabled inAgentEnabled);
}
