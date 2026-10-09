package org.entermediadb.ai.skills;

import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.agentjobs.AgentJob;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.markdown.MarkdownUtil;
import org.openedit.OpenEditException;

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

		//Show progress until the job is done, then go back to the chat monitor
		for (int loops = 0; ; loops++)
		{
			if (loops > 500)
			{
				throw new OpenEditException("Agent job status skill has looped too many times. Something is wrong.");
			}
			agentjob = (AgentJob) getMediaArchive().getCachedData("agentjob", agentjob.getId());
			LlmResponse response = renderStatus(inContext, agentjob);

			//Complete means the answer is listed within the final text than can be added to the context for the next step
			String status = agentjob.get("status");
			if ("complete".equals(status) || "error".equals(status))
			{
				String startup_scenario = (String) inContext.getChannel().getValue("startup_scenario");
				response.setNextAutomationStep(startup_scenario);
				inContext.setLastResponse(response);
				break;
			}
			log.info("Agent job not completed yet.");
			inContext.setLastResponse(response);
			processUpdate(inContext);
			if (!waitBeforeResponding(inContext, 5000L))
			{
				noResponse(inContext);
				return;
			}
		}

		super.process(inContext);
	}

	protected LlmResponse renderStatus(AgentContext inContext, AgentJob inAgentJob)
	{
		inContext.put("agentjob", inAgentJob);
		MarkdownUtil markdown = new MarkdownUtil();
		inContext.put("markdown", markdown);

		Date endtime = inAgentJob.getDate("enddate");
		Date starttime = inAgentJob.getDate("submitteddate");
		if( endtime != null && starttime != null)
		{
			double secondsTaken = (endtime.getTime() - starttime.getTime()) / 5000D;
			//Only local. add putLocal
			inContext.getContext().put("secondstaken", secondsTaken);
		}

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		return llmconnection.renderLocalAction(inContext, "agent_job_showjobplan");
	}
}
