package org.entermediadb.ai;

import org.entermediadb.ai.llm.AgentEnabled;

public interface SkillStatusListener
{
	void fireStatusStarting(AgentContext inContext, AgentEnabled inAgentEnabled);

	void fireStatusComplete(AgentContext inContext, AgentEnabled inAgentEnabled);
}
