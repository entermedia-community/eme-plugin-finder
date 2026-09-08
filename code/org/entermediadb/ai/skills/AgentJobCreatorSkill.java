package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.classify.EmbeddingManager;
import org.entermediadb.ai.llm.AutomationStep;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class AgentJobCreatorSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(AgentJobCreatorSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		MultiValued agentmessage = messageContext.getAgentMessage();
		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));

		String userRequest = usermessage.get("message");
		// inAgentContext.get("goaltext"); //required

		// Needed?
		messageContext.fireStatusStarting(messageContext.getCurrentAutomationStep());

		Collection<String> docids = loadSkillDocIds();
		EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");

		String prompt =
			"You are an AI agent orchestrator tasked with creating tasks to complete a given request. Chose the appropriate skills and automation scenarios to accomplish the goal. Only return the doc ids";

		LlmResponse response = embeddings.callFindDocIds(messageContext, docids, prompt, userRequest);

		// String responseText = response.getMessage();

		JSONObject raw = response.getRawResponse();
		JSONArray selecedSkills = (JSONArray) raw.get("parent_ids");

		if (selecedSkills != null && !selecedSkills.isEmpty())
		{
			// Pick first skill only?
			String firstSkillId = (String) selecedSkills.get(0);
			if (firstSkillId != null && firstSkillId.startsWith("automationscenario"))
			{
				String scenarioid = firstSkillId.replace("automationscenario_", "");

				BasicLlmResponse basicResponse = new BasicLlmResponse();
				basicResponse.setMessage("Agent Job Skill scenario picked: " + scenarioid);
				inAgentContext.setLastResponse(basicResponse);
				AutomationStep skillEnabled = messageContext.getCurrentAutomationStep();
				messageContext.fireStatusComplete(skillEnabled);

				/*
				 * RunningScenario running = (RunningScenario) getMediaArchive().getBean("runningscenario", false);
				 * running.setId(scenarioid);
				 * 
				 * AutomationStep stepEnabled = running.findEnabled(scenarioid); if (stepEnabled == null) {
				 * log.error("No step enabled found for id: " + scenarioid); return; }
				 * running.runProcess(stepEnabled, inAgentContext);
				 * messageContext.putContextValue("selectedScenarioId", scenarioid);
				 */
			}
			else if (firstSkillId != null && firstSkillId.startsWith("aiskill"))
			{
				String skillid = firstSkillId.replace("aiskill_", "");
				BasicLlmResponse basicResponse = new BasicLlmResponse();
				basicResponse.setMessage("Agent Job selected skill: " + skillid);
				inAgentContext.setLastResponse(basicResponse);
				AutomationStep skillEnabled = messageContext.getCurrentAutomationStep();
				messageContext.fireStatusComplete(skillEnabled);

			}

		}

		// Parse this as JSON?

		// messageContext.putContextValue("goal", goal);
		super.process(messageContext);

		// AutomationStep skillEnabled = messageContext.getCurrentAgentEnable();
		// fireStatusComplete(skillEnabled);

		// getMediaArchive().fireSharedMediaEvent("goaltask/goalcreated");

		return;
	}

	protected Collection<String> loadSkillDocIds()
	{
		Collection<String> docids = (Collection<String>) getMediaArchive().getCacheManager().get("skills", "skillids");
		if (docids == null)
		{
			docids = new ArrayList<String>();

			HitTracker skills = getMediaArchive().getList("agentskill");
			Collection<String> skillids = skills.collectValues("id");
			for (String id : skillids)
			{
				String typed = "aiskill_" + id;
				docids.add(typed);
			}

			HitTracker automations = getMediaArchive().getList("automationscenario");
			Collection<String> moreids = automations.collectValues("id");
			for (String id : moreids)
			{
				docids.add("automationscenario_" + id);
			}
			getMediaArchive().getCacheManager().put("skills", "skillids", docids);
		}
		return docids; // Replace with actual collection of document IDs
	}

}
