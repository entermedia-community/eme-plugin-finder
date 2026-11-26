package org.entermediadb.ai.agentjobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.skills.OpenCodeRunnerSkill;
import org.entermediadb.ai.skills.OpenCodeStatusSkill;
import org.entermediadb.mcp.client.OpenCodeClient;
import org.entermediadb.mcp.client.SessionStatus;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

/**
 * Sends each step's request to an opencode session (via openCodeRunnerSkill) instead of running
 * the step's aiskill or automation scenario in Java.
 */
public class OpenCodeOrchestrator extends BaseAgentJobOrchestrator
{
	private static final Log log = LogFactory.getLog(OpenCodeOrchestrator.class);
	private static final long POLL_INTERVAL_MS = 2000L;

	public static class SessionUnconfirmedException extends OpenEditException
	{
		public SessionUnconfirmedException(String inMessage, Throwable inCause)
		{
			super(inMessage, inCause);
		}
	}

	protected void processStep(AgentJobRunnable inAgentJob, MultiValued inStep)
	{
		String userrequest = findUserRequest(inAgentJob.getAgentJob(), inStep);
		if (userrequest == null)
		{
			throw new OpenEditException("OpenCodeOrchestrator: no request to send to opencode for step " + inStep.getId());
		}
		//openCodeRunnerSkill reads the prompt from the step and keeps it for resumes and repeats
		inStep.setValue("userrequest", userrequest);
		inStep.setValue("status", "running");
		getMediaArchive().saveData("agentjobstep", inStep);

		inAgentJob.getContext().put("agentjobstep", inStep);
		inAgentJob.getContext().put("userrequest", userrequest);
		inAgentJob.getContext().setCurrentAgentJobStep(inAgentJob.getAgentJob().findEnabled(inStep.getId()));

		OpenCodeClient client = getAgentJobManager().getOpenCodeClient();
		client.connectToServer();
		try
		{
			if (!client.awaitEventStream(10000L))
			{
				throw new OpenEditException("OpenCodeOrchestrator: event stream did not connect");
			}
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new OpenEditException("OpenCodeOrchestrator: interrupted waiting for event stream", ex);
		}

		OpenCodeRunnerSkill skill = (OpenCodeRunnerSkill) getModuleManager().getBean(getCatalogId(), "openCodeRunnerSkill");
		try
		{
			skill.startSession(inAgentJob.getContext());
			waitForSession(inAgentJob, inStep, client);
		}
		catch (RuntimeException ex)
		{
			SessionStatus session = client.loadStatus(inStep.getId());
			if (session == null || !session.isCompleted())
			{
				String sessionId = session == null ? "for step " + inStep.getId() : session.getSessionId();
				throw new SessionUnconfirmedException("OpenCode session " + sessionId
						+ " may still be running; verify it before retrying: " + ex.getMessage(), ex);
			}
			throw ex;
		}
	}

	protected void waitForSession(AgentJobRunnable inAgentJob, MultiValued inStep, OpenCodeClient client)
	{
		SessionStatus session = client.loadStatus(inStep.getId());
		if (session == null)
		{
			throw new OpenEditException("OpenCodeOrchestrator: no session for step " + inStep.getId());
		}
		while (!inAgentJob.isCancelled())
		{
			try
			{
				session = client.advanceSession(inAgentJob.getContext().getScriptLogger(), inStep.getId(), POLL_INTERVAL_MS);
				if (session == null)
				{
					throw new OpenEditException("OpenCodeOrchestrator: lost session for step " + inStep.getId());
				}
				if (session.getConfirmationPendingId() != null)
				{
					savePendingQuestion(inStep, session);
					Thread.sleep(POLL_INTERVAL_MS);
					continue;
				}
				clearPendingQuestion(inStep);
				if (session.isCompleted())
				{
					if (session.getError() != null)
					{
						throw new OpenEditException("OpenCode session " + session.getSessionId() + " failed: " + session.getError());
					}
					try
					{
						String output = OpenCodeStatusSkill.extractLastAssistantText(client.getMessages(session.getSessionId()));
						if (output != null)
						{
							inStep.setValue("markdowncontent", output);
						}
					}
					catch (Exception ex)
					{
						log.warn("OpenCode session " + session.getSessionId() + " completed, but its output could not be loaded", ex);
					}
					completeIfRunning(inStep);
					return;
				}
			}
			catch (InterruptedException ex)
			{
				Thread.currentThread().interrupt();
				throw new OpenEditException("OpenCodeOrchestrator: interrupted waiting for session " + session.getSessionId(), ex);
			}
		}
	}

	protected void savePendingQuestion(MultiValued inStep, SessionStatus inSession)
	{
		String pendingId = inSession.getConfirmationPendingId();
		if (pendingId.equals(inStep.get("confirmationpendingid")))
		{
			return;
		}
		inStep.setValue("status", inSession.getPendingStatus());
		inStep.setValue("pendingquestion", inSession.getCurrentQuestion());
		inStep.setValue("confirmationpendingid", pendingId);
		JSONObject form = inSession.getPendingForm();
		inStep.setValue("pendingform", form == null ? null : form.toJSONString());
		getMediaArchive().saveData("agentjobstep", inStep);
	}

	protected void clearPendingQuestion(MultiValued inStep)
	{
		if (inStep.get("confirmationpendingid") == null)
		{
			return;
		}
		inStep.setValue("status", "running");
		inStep.setValue("pendingquestion", null);
		inStep.setValue("confirmationpendingid", null);
		inStep.setValue("pendingform", null);
		getMediaArchive().saveData("agentjobstep", inStep);
	}

	protected String findUserRequest(AgentJob inJob, MultiValued inStep)
	{
		String[] values = new String[] { inStep.get("userrequest"), inStep.get("markdowncontent"), inJob.get("userrequest") };
		for (String value : values)
		{
			if (value != null && !value.trim().isEmpty())
			{
				return value;
			}
		}
		return null;
	}
}
