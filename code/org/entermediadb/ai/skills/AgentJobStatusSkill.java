package org.entermediadb.ai.skills;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.agentjobs.AgentJob;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;

public class AgentJobStatusSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(AgentJobStatusSkill.class);

	@Override
	public void process(AgentContext inContext)
	{
		log.info("AgentJobStatusSkill running for catalog: " + getCatalogId());

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inContext, "agent_job_showjobplan");
		AgentJob agentjob = (AgentJob) inContext.getContextValue("agentjob");
		String status = agentjob.get("status");
		if (!"completed".equals(status) || !"error".equals(status))
		{
			log.info("Agent job completed successfully.");
			inContext.setWaitTime(1000L);
			response.setNextSkillEnabled("agentJobStatus");
		}	
		inContext.setLastResponse(response);

		super.process(inContext);
	}
}
