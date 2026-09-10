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
		AgentJob agentjob = (AgentJob) inContext.getContextValue("agentjob");
		if( agentjob == null)
		{
			log.warn("No agent job found in context.");
			return;
		}

		agentjob = (AgentJob)getMediaArchive().getData("agentjob", agentjob.getId());
		inContext.put("agentjob", agentjob);
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inContext, "agent_job_showjobplan");

		String status = agentjob.get("status");
		if (!"complete".equals(status) && !"error".equals(status))
		{
			log.info("Agent job not completed yet.");
			inContext.setWaitTime(1000L);
			response.setRunSkillEnabled("agentJobStatus");
		}	
		else
		{
			response.setNextSkillEnabled("chatMonitor");
		}
		inContext.setLastResponse(response);

		super.process(inContext);
	}
}
