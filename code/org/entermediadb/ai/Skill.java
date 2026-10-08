package org.entermediadb.ai;

public interface Skill
{
	public void processStarting(AgentContext inContext);

	public void process(AgentContext inContext);

	public void processCompleted(AgentContext inContext);

	

}
