package org.entermediadb.ai.skills;

import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.agentjobs.AgentJob;
import org.entermediadb.ai.agentjobs.AgentJobManager;
import org.entermediadb.ai.agentjobs.AgentJobStep;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.mcp.client.OpenCodeClient;
import org.entermediadb.mcp.client.SessionStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.OpenEditException;

/**
 * OpenCodeStatusSkill - Runs after OpenCodeRunnerSkill and advances its opencode session, showing
 * progress until the session goes idle, hits an error, or opencode asks a permission question
 * that needs a human answer.
 */
public class OpenCodeStatusSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(OpenCodeStatusSkill.class);

	// How often the status is re-rendered while opencode works.
	protected static final long POLL_INTERVAL_MS = 2000L;

	// Overall wall-clock budget for one invocation before giving up as timed out.
	protected static final long MAX_WAIT_MS = 20 * 60 * 1000L;

	@Override
	public void process(AgentContext inContext)
	{
		log.info("OpenCodeStatusSkill running for catalog: " + getCatalogId());
		AgentJob agentjob = (AgentJob) inContext.getCurrentAgentJob();
		if( agentjob == null)
		{
			log.warn("No agent job found in context.");
			return;
		}

		SessionStatus sessionstatus = findSessionStatus(inContext);
		if (sessionstatus == null || sessionstatus.getAgentJobStep() == null)
		{
			// No opencode session yet (e.g. entity_chat_custom_skills runs us before the runner), so follow the job's status
			waitForAgentJob(inContext, agentjob);
			return;
		}
		String runnerstepid = sessionstatus.getAgentJobStep().getId();
		AgentJobManager manager = (AgentJobManager) getMediaArchive().getBean("agentJobManager");
		OpenCodeClient client = manager.getOpenCodeClient();

		//Show progress until the session is done, then go back to the chat monitor
		long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
		while (true)
		{
			try
			{
				sessionstatus = client.advanceSession(inContext.getScriptLogger(), runnerstepid, 0L);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new OpenEditException("OpenCodeStatusSkill interrupted waiting on opencode session " + sessionstatus.getSessionId(), e);
			}
			inContext.put("sessionstatus", sessionstatus);
			// Save progress from time to time so the step shows what opencode has said so far.
			saveMarkdown(client, sessionstatus);

			if (sessionstatus.getConfirmationPendingId() != null)
			{
				savePendingQuestion(inContext, sessionstatus);
				inContext.setLastResponse(renderStatus(inContext, agentjob));
				break;
			}

			if (sessionstatus.isCompleted())
			{
				if (sessionstatus.getError() != null)
				{
					throw new OpenEditException("OpenCodeStatusSkill: opencode session " + sessionstatus.getSessionId()
							+ " failed: " + sessionstatus.getError());
				}
				String output = extractLastAssistantText(client.getMessages(sessionstatus.getSessionId()));
				saveMarkdown(sessionstatus, output);
				inContext.put("commandoutput", output);
				inContext.put("markdowncontent", output);

				LlmResponse response = renderStatus(inContext, agentjob);
				String startup_scenario = (String) inContext.getChannel().getValue("startup_scenario");
				response.setNextAutomationStep(startup_scenario); //go back to creating? eme_server_custom_skills_confirm
				inContext.setLastResponse(response);
				break;
			}

			if (System.currentTimeMillis() > deadline)
			{
				throw new OpenEditException("OpenCodeStatusSkill: opencode session " + sessionstatus.getSessionId()
						+ " did not finish within " + MAX_WAIT_MS + "ms");
			}

			log.info("OpenCode session not completed yet.");
			inContext.setLastResponse(renderStatus(inContext, agentjob));
			processUpdate(inContext); //This fires an event that might refresh the UI with a chat broadcast

			if (!waitBeforeResponding(inContext, POLL_INTERVAL_MS))
			{
				noResponse(inContext);
				return;
			}
		}

		super.process(inContext);
	}

	/** Show progress until the job is done, then go back to the chat monitor */
	protected void waitForAgentJob(AgentContext inContext, AgentJob inAgentJob)
	{
		for (int loops = 0; ; loops++)
		{
			if (loops > 500)
			{
				throw new OpenEditException("Agent job status skill has looped too many times. Something is wrong.");
			}
			LlmResponse response = renderStatus(inContext, inAgentJob);

			//Complete means the answer is listed within the final text than can be added to the context for the next step
			String status = inAgentJob.get("status");
			if ("complete".equals(status) || "error".equals(status))
			{
				String startup_scenario = (String) inContext.getChannel().getValue("startup_scenario");
				response.setNextAutomationStep(startup_scenario);
				inContext.setLastResponse(response);
				break;
			}
			log.info("Agent job not completed yet.");
			inContext.setLastResponse(response);
			processUpdate(inContext); //This fires an event that might refresh the UI with a chat broadcast 

			if (!waitBeforeResponding(inContext, POLL_INTERVAL_MS))
			{
				noResponse(inContext);
				return;
			}
		}
		super.process(inContext);
	}

	/**
	 * opencode is blocked on a tool-call approval. Save the question onto the runner's step and
	 * leave it for a human to answer instead of marking it complete; the orchestrator only stamps
	 * "complete" when the step's status is still "running".
	 */
	protected void savePendingQuestion(AgentContext inContext, SessionStatus inStatus)
	{
		Data runnerstep = inStatus.getAgentJobStep();
		// "question" or "securityprompt" (see jobstatus.xml); the prompt text replaces the markdown.
		String pendingStatus = inStatus.getPendingStatus() != null ? inStatus.getPendingStatus() : "securityprompt";
		runnerstep.setValue("status", pendingStatus);
		runnerstep.setValue("markdowncontent", inStatus.getCurrentQuestion());
		runnerstep.setValue("pendingquestion", inStatus.getCurrentQuestion());
		runnerstep.setValue("confirmationpendingid", inStatus.getConfirmationPendingId());
		// The form's fields are rendered by agent_job_showjobplan.html via AgentJob.getPendingForm
		JSONObject form = inStatus.getPendingForm();
		runnerstep.setValue("pendingform", form == null ? null : form.toJSONString());
		getMediaArchive().saveData("agentjobstep", runnerstep);
		inContext.put("pendingform", form);
	}

	/** The runner put sessionstatus in its own (parent) context, which getContextValue does not search */
	protected SessionStatus findSessionStatus(AgentContext inContext)
	{
		for (AgentContext ctx = inContext; ctx != null; ctx = ctx.getParentContext())
		{
			Object status = ctx.getContext().get("sessionstatus");
			if (status instanceof SessionStatus)
			{
				return (SessionStatus) status;
			}
		}
		Object status = inContext.getContextValue("sessionstatus");
		if (status instanceof SessionStatus)
		{
			return (SessionStatus) status;
		}
		// Resumed after an answer or approval: look the session up by the runner's step id
		String stepid = (String) inContext.getContextValue("currentagentjobstepid");
		if (stepid == null)
		{
			Data step = (Data) inContext.getContextValue("agentjobstep");
			stepid = step == null ? null : step.getId();
		}
		if (stepid == null)
		{
			return null;
		}
		AgentJobManager manager = (AgentJobManager) getMediaArchive().getBean("agentJobManager");
		return manager.getOpenCodeClient().loadStatus(stepid);
	}

	/**
	 * The function name (enabled id) of the step in inAgentJob that runs inAiSkill, such as
	 * eme_server_custom_skills_answer or openCodeAnswer, so templates post to the right scenario.
	 */
	public static String findFunctionForSkill(AgentJob inAgentJob, String inAiSkill, String inDefault)
	{
		if (inAgentJob == null)
		{
			return inDefault;
		}
		for (AgentJobStep step : inAgentJob.getAllSteps())
		{
			String aiskill = step.getAgentJobStepData().get("aiskill");
			if (aiskill == null && step.getAgentData() != null)
			{
				aiskill = step.getAgentData().getId();
			}
			if (inAiSkill.equals(aiskill))
			{
				return step.getEnabledId();
			}
		}
		return inDefault;
	}

	/**
	 * Fetches the session's latest assistant text and saves it as the step's markdowncontent.
	 * Best effort: a failure here must not break the running session.
	 */
	protected void saveMarkdown(OpenCodeClient inClient, SessionStatus inStatus)
	{
		try
		{
			saveMarkdown(inStatus, extractLastAssistantText(inClient.getMessages(inStatus.getSessionId())));
		}
		catch (Exception e)
		{
			log.warn("OpenCodeStatusSkill could not save progress for session " + inStatus.getSessionId(), e);
		}
	}

	protected void saveMarkdown(SessionStatus inStatus, String inMarkdown)
	{
		if (inMarkdown == null || inMarkdown.isEmpty() || inStatus.getAgentJobStep() == null)
		{
			return;
		}
		Data step = inStatus.getAgentJobStep();
		if (inMarkdown.equals(step.get("markdowncontent")))
		{
			return;
		}
		step.setValue("markdowncontent", inMarkdown);
		getMediaArchive().saveData("agentjobstep", step);
	}

	/**
	 * Finds the most recent assistant message (per the opencode SDK's {info: {role}, parts:
	 * [{type, text}]} message shape) and concatenates its text parts.
	 */
	protected String extractLastAssistantText(List<JSONObject> inMessages)
	{
		if (inMessages == null)
		{
			return null;
		}
		for (int i = inMessages.size() - 1; i >= 0; i--)
		{
			JSONObject message = inMessages.get(i);
			Object infoObj = message.get("info");
			if (!(infoObj instanceof JSONObject) || !"assistant".equals(((JSONObject) infoObj).get("role")))
			{
				continue;
			}
			Object partsObj = message.get("parts");
			if (!(partsObj instanceof JSONArray))
			{
				continue;
			}
			StringBuilder text = new StringBuilder();
			for (Object partObj : (JSONArray) partsObj)
			{
				if (partObj instanceof JSONObject && "text".equals(((JSONObject) partObj).get("type")))
				{
					Object partText = ((JSONObject) partObj).get("text");
					if (partText != null)
					{
						text.append(partText);
					}
				}
			}
			return text.toString();
		}
		return null;
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
			double secondsTaken = (endtime.getTime() - starttime.getTime()) / 1000D;
			//Only local. add putLocal
			inContext.getContext().put("secondstaken", secondsTaken);
		}

		inContext.put("currentagentjobstep",inContext.getCurrentAgentJobStep());
		// The form and approve buttons stay in this job's scenario; posting another scenario starts a new job
		inContext.put("currentscenario", inAgentJob.getScenarioId());
		inContext.put("answerfunction", findFunctionForSkill(inAgentJob, "openCodeAnswerSkill", "openCodeAnswer"));
		inContext.put("approvefunction", findFunctionForSkill(inAgentJob, "openCodeApproveSkill", "openCodeApprove"));
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		return llmconnection.renderLocalAction(inContext, "agent_job_showstatus");
	}
}
