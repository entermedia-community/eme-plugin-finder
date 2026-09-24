package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.agentjobs.AgentJobOrchestrator;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.mcp.client.OpenCodeClient;
import org.entermediadb.mcp.client.SessionStatus;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

/**
 * OpenCodeAnswerSkill - Answers an opencode question for an agentjobstep.
 * Context: "agentjobstepid" (required), "answer" (required).
 */
public class OpenCodeAnswerSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		String stepid = (String) inContext.getContextValue("agentjobstepid");
		MultiValued step = (MultiValued) getMediaArchive().getData("agentjobstep", stepid);
		if (step == null || !"question".equals(step.get("status")))
		{
			throw new OpenEditException("OpenCodeAnswerSkill: step " + stepid + " is not waiting on a question");
		}
		String answer = (String) inContext.getContextValue("answer");
		if (answer == null || answer.trim().isEmpty())
		{
			throw new OpenEditException("OpenCodeAnswerSkill: No answer provided");
		}

		AgentJobOrchestrator orchestrator = (AgentJobOrchestrator) getMediaArchive().getBean("agentJobOrchestrator");
		OpenCodeClient client = orchestrator.getOpenCodeClient();
		SessionStatus status = client.loadStatus(stepid);
		String requestid = step.get("pendingpermissionid");
		if (status == null || requestid == null)
		{
			throw new OpenEditException("OpenCodeAnswerSkill: no opencode session waiting for step " + stepid);
		}
		if (!client.replyToQuestion(status.getSessionId(), requestid, answer))
		{
			throw new OpenEditException("OpenCodeAnswerSkill: opencode rejected the reply for step " + stepid);
		}

		step.setValue("status", "running");
		step.setValue("pendingquestion", null);
		step.setValue("pendingpermissionid", null);
		getMediaArchive().saveData("agentjobstep", step);

		LlmResponse response = new BasicLlmResponse();
		response.setMessage("Question answered: " + answer);
		inContext.setLastResponse(response);
		super.process(inContext);
	}
}
