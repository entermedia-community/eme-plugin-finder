package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.BasicLlmResponse;

public class MyEmeUploadSkillsMemorySkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		//TODO: not implemented yet
		BasicLlmResponse response = new BasicLlmResponse();
		response.setMessage("Upload Skills and Memory is not implemented yet.");
		inContext.setLastResponse(response);
		super.process(inContext);
	}
}
