package org.entermediadb.ai.skills;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.agentjobs.AgentJobManager;
import org.entermediadb.ai.agentjobs.AgentJobStep;
import org.entermediadb.mcp.client.OpenCodeClient;
import org.entermediadb.mcp.client.SessionStatus;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;

/**
 * OpenCodeRunnerSkill - Starts (or resumes) an opencode server session for the current
 * agentjobstep. OpenCodeStatusSkill then waits until the session goes idle, hits an error, or
 * opencode asks a permission question that needs a human answer.
 *
 * Usage in AgentContext: - "userrequest" (required, set by OpenCodeUserRequestSkill in chat) - The prompt sent to opencode - "workingpath"
 * (optional) - Directory the opencode session runs in; defaults to one level above
 * getMediaArchive().getRootDirectory() - "agentjobstep" - The MultiValued step record; its id keys
 * the OpenCodeClient session map so re-running this skill resumes the same opencode session
 * instead of starting a new one.
 *
 * Results: - "sessionstatus" is put in the context and super.process runs OpenCodeStatusSkill.
 * - On completion, OpenCodeStatusSkill puts the assistant's final reply text in "commandoutput".
 * - If opencode raises a permission request (a tool call awaiting approval), OpenCodeStatusSkill
 * leaves the agentjobstep with status "question" or "securityprompt" plus "pendingquestion"/"confirmationpendingid".
 * Answering that question (via OpenCodeClient#replyToPermission) and re-running this step resumes the same session.
 */
public class OpenCodeRunnerSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(OpenCodeRunnerSkill.class);

	@Override
	public void process(AgentContext inContext)
	{
		AgentJobManager manager = (AgentJobManager) getMediaArchive().getBean("agentJobManager");
		
		String workingpath = (String) inContext.getContextValue("workingpath");
		if (workingpath == null || workingpath.trim().isEmpty())
		{
			ContentItem root = getMediaArchive().getPageManager().getRepository().get("/");

			workingpath = new File(root.getAbsolutePath()).getParentFile().getAbsolutePath();

			log.info("OpenCodeRunnerSkill: No workingpath provided, defaulting to " + workingpath);
		}

		AgentJobStep agentjobstep = inContext.getCurrentAgentJobStep();
		String query = (String) agentjobstep.get("userrequest");
		if( query == null || query.trim().isEmpty()) {
			query = (String)inContext.getContextValue("userrequest");
		}
		SessionStatus sessionstatus;
		try
		{
			OpenCodeClient client = manager.getOpenCodeClient();
			client.connectToServer(); //start listening
			sessionstatus = client.loadStatus(agentjobstep.getId());
			if (sessionstatus == null)
			{
				sessionstatus = client.startSessionId(agentjobstep, workingpath, query);
			}
		}
		catch (Exception e)
		{
			log.error("OpenCodeRunnerSkill error running opencode in " + workingpath, e);
			throw new OpenEditException("OpenCodeRunnerSkill error running opencode in " + workingpath, e);
		}
		// OpenCodeStatusSkill runs next as our child and waits on the session
		inContext.put("sessionstatus", sessionstatus);

		super.process(inContext);
	}

}
