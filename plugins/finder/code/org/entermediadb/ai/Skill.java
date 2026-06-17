package org.entermediadb.ai;

public interface Skill
{
	public void processstart(AgentContext inContext);

	public void processend(AgentContext inContext);

	void process(AgentContext inContext);

}
