package org.entermediadb.ai;

public interface Skill
{
	public void processStarting(AgentContext inContext);

	public void process(AgentContext inContext);

	/** Broadcasts the last response while the skill keeps working */
	public void processUpdate(AgentContext inContext);

	/** Broadcasts the last response, then runs its exec step */
	public void processCompleted(AgentContext inContext);

}
