package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.agentjobs.AgentJobManager;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.mcp.client.OpenCodeClient;
import org.entermediadb.mcp.client.SessionStatus;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

/**
 * OpenCodeApproveSkill - Answers an opencode security (permission) prompt for an agentjobstep.
 * Context: "agentjobstep" (required), "decision" = once | always | reject (default once).
 */
public class OpenCodeApproveSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		MultiValued step = (MultiValued) inContext.getContextValue("agentjobstep");
		if (step == null)
		{
			throw new OpenEditException("OpenCodeApproveSkill: missing agentjobstep in context");
		}
		if (!"securityprompt".equals(step.get("status")))
		{
			//throw new OpenEditException("OpenCodeApproveSkill: step " + step.get("id") + " is not waiting on a security prompt");
			super.process(inContext);
			return;
		}
		String decision = (String) inContext.getContextValue("decision");
		if (!"always".equals(decision) && !"reject".equals(decision))
		{
			decision = "once";
		}

		AgentJobManager manager = (AgentJobManager) getMediaArchive().getBean("agentJobManager");
		OpenCodeClient client = manager.getOpenCodeClient();
		SessionStatus status = client.loadStatus(step.get("id"));
		String requestid = step.get("pendingpermissionid");
		if (status == null || requestid == null)
		{
			throw new OpenEditException("OpenCodeApproveSkill: no opencode session waiting for step " + step.get("id"));
		}
		if (!client.replyToPermission(status.getSessionId(), requestid, decision))
		{
			throw new OpenEditException("OpenCodeApproveSkill: opencode rejected the reply for step " + step.get("id"));
		}

		step.setValue("status", "running");
		step.setValue("pendingquestion", null);
		step.setValue("pendingpermissionid", null);
		getMediaArchive().saveData("agentjobstep", step);

		LlmResponse response = new BasicLlmResponse();
		response.setMessage("Security prompt answered: " + decision);
		inContext.setLastResponse(response);
		super.process(inContext);
	}
}
