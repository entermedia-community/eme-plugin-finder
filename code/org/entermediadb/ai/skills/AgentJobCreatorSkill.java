package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.classify.EmbeddingManager;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class AgentJobCreatorSkill extends BaseSkill
{
    private static final Log log = LogFactory.getLog(AgentJobCreatorSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		String userRequest = inAgentContext.get("goaltext"); //required

		//Needed?
		messageContext.fireStatusStarting(messageContext.getCurrentAutomationStep());

		MultiValued agentmessage = messageContext.getAgentMessage();
		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));

		Collection<String> docids = loadSkillDocIds();
		EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");

		String prompt = "You are an AI agent orchestrator tasked with creating tasks to complete a given request. Chose the appropriate skills and automation scenarios to accomplish the goal. Only return the doc ids";


		LlmResponse response = embeddings.callFindDocIds(messageContext, docids, prompt, userRequest);

		String responseText = response.getMessage();

		//Parse this as JSON?
		

		//messageContext.putContextValue("goal", goal);
		super.process(messageContext);

		// AutomationStep skillEnabled = messageContext.getCurrentAgentEnable();
		// fireStatusComplete(skillEnabled);

		// getMediaArchive().fireSharedMediaEvent("goaltask/goalcreated");

		return;
	}

	protected Collection<String> loadSkillDocIds()
	{
		Collection<String> docids = (Collection<String> )getMediaArchive().getCacheManager().get("skills","skillids");
		if(docids == null)
		{
			docids = new ArrayList<String>();

			HitTracker skills = getMediaArchive().getList("agentskill");
			Collection<String> skillids = skills.collectValues("id");
			for(String id : skillids)
			{
				String typed = "aiskill_" + id;
				docids.add(typed);
			}

			HitTracker automations = getMediaArchive().getList("automationscenario");
			Collection<String> moreids = automations.collectValues("id");
			for(String id : moreids)
			{
				docids.add("automationscenario_" + id);
			}
			getMediaArchive().getCacheManager().put("skills","skillids", docids);
		}
		return docids; // Replace with actual collection of document IDs
	}
	
}
