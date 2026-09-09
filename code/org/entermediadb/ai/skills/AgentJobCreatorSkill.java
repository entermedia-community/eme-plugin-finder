package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.automation.PossibleStep;
import org.entermediadb.ai.classify.EmbeddingManager;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;

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
		inAgentContext.put("query", userRequest); // required

		// Needed?
		messageContext.fireStatusStarting(messageContext.getCurrentAutomationStep());

		Collection<String> docids = loadSkillDocIds();
		EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");

		//TODO: Add more context and have AI create a summary of what the user wants. Could be across multiuple messages
		String prompt =
			"You are a search tool looking for tools needed to accomplish a goal. Only return the doc ids";

		LlmResponse response = embeddings.callFindDocIds(messageContext, docids, prompt, userRequest);

		JSONObject raw = response.getRawResponse();
		Collection<String> selecedSkills = (Collection<String>) raw.get("parent_ids");
		// TODO: Update the UI with a notice that we are making progress

		Collection<PossibleStep> possible_steps = new ArrayList<PossibleStep>();

		if (selecedSkills != null && !selecedSkills.isEmpty())
		{
			// Pick first skill only?
			for (String firstSkillId : selecedSkills)
			{

				if (firstSkillId != null && firstSkillId.startsWith("automationscenario"))
				{
					String scenarioid = firstSkillId.replace("automationscenario_", "");
					Data scenario = getMediaArchive().getCachedData("automationscenario", scenarioid);
					PossibleStep step = new PossibleStep();
					step.setId(firstSkillId);
					step.setValue("description", scenario.get("longdescription"));
					step.setValue("inputs", scenario.get("parameters"));
					step.setValue("outputs", scenario.get("defaultoutput"));
					possible_steps.add(step);
				}
				else if (firstSkillId != null && firstSkillId.startsWith("aiskill"))
				{
					String skillid = firstSkillId.replace("aiskill_", "");
					Data skill = getMediaArchive().getCachedData("aiskill", skillid);
					PossibleStep step = new PossibleStep();
					step.setId(firstSkillId);
					step.setValue("description", skill.get("markdowncontent"));
					step.setValue("inputs", skill.get("parameters"));
					step.setValue("outputs", skill.get("defaultoutput"));
					possible_steps.add(step);
				}
			}
			inAgentContext.put("possible_steps", possible_steps);
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
			LlmResponse planresponse = llmconnection.callStructure(messageContext, "agentJobCreator");

			JSONObject payload = planresponse.getResponsePayload();

			// save to the database
			Data newjob = getMediaArchive().getSearcher("agentjob").createNewData();
			newjob.setValue("owner", inAgentContext.getChatUser());
			newjob.setValue("submitteddate", new Date());
			newjob.setValue("status", "new");
			newjob.setValue("llmprompt", userRequest);
			getMediaArchive().saveData("agentjob", newjob);
			Collection<Map> steps = (Collection<Map>) payload.get("agent_steps");
			saveSteps(newjob, steps);

			// TODO: Confirm with the user. render local to tell the user what we are going to kick off. Dont go
			// forward without confirmation skill

			// TODO: Once saved Put a link to the Job Orchestrator to monitor the job. Or have this job listen
			// to web events and refresh?

			// Kick off the job scheduler?
			getMediaArchive().fireSharedMediaEvent("ai/runopenjobs");


			//next Render the confirmation and wait

			super.process(inAgentContext);

		}
	}

	protected void saveSteps(Data newjob, Collection<Map> steps)
	{
		Searcher searcher = getMediaArchive().getSearcher("agentjobstep");
		Collection tosave = new ArrayList();
		int ordering = 0;
		for (Map stepData : steps)
		{
			Data step = searcher.createNewData();
			step.setValue("agentjob", newjob.getId());
			step.setValue("ordering", ordering++);

			String id = (String)stepData.get("skill_id");
			if( id.startsWith("aiskill_"))
			{
				String skillid = id.replace("aiskill_", "");
				step.setValue("aiskillid", skillid);
			}
			else if( id.startsWith("automationscenario_"))
			{
				String scenarioid = id.replace("automationscenario_", "");
				step.setValue("workflowid", scenarioid);
			}

			step.setValue("markdowncontent", stepData.get("details"));
			JSONArray inputs = (JSONArray) stepData.get("parameters");
			if( inputs != null)
			{
				step.setValue("parameters", inputs.toJSONString());	
			}

			//step.setValue("defaultoutput", stepData.get("outputs"));
			tosave.add(step);
		}
		getMediaArchive().saveData("agentjobstep", tosave);

	}

	protected Collection<String> loadSkillDocIds()
	{
		Collection<String> docids = (Collection<String>) getMediaArchive().getCacheManager().get("skills", "skillids");
		if (docids == null)
		{
			docids = new ArrayList<String>();

			//TODO: Add embeded checks
			Collection<MultiValued> skills = getMediaArchive().query("aiskill").all().search();

			for (MultiValued data : skills)
			{
				if (data.get("markdowncontent") != null)
				{
					String typed = "aiskill_" + data.getId();
					docids.add(typed);
				}
			}

			Collection<MultiValued> automations = getMediaArchive().query("automationscenario").all().cachedSearch();

			for (MultiValued data : automations)
			{
				if (data.get("markdowncontent") != null)
				{
					docids.add("automationscenario_" + data.getId());
				}
			}
			getMediaArchive().getCacheManager().put("skills", "skillids", docids);
		}
		return docids; // Replace with actual collection of document IDs
	}

}
