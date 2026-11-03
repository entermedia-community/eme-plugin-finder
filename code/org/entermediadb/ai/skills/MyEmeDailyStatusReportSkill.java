package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.BasicLlmResponse;

public class MyEmeDailyStatusReportSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		//TODO: not implemented yet
		BasicLlmResponse response = new BasicLlmResponse();
		response.setMessage("Daily Status Report is not implemented yet.");
		inContext.setLastResponse(response);
		super.process(inContext);
	}
}
