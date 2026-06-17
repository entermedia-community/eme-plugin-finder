package org.entermediadb.ai;

public interface SkillStatusListener
{
	void fireStatusStarting(AgentContext inContext, String inMessage);

	void fireStatusComplete(AgentContext inContext, String inMessage);
}
