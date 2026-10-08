package org.entermediadb.ai;

import java.util.ArrayList;
import java.util.Collection;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.agentjobs.AgentJobStep;
import org.openedit.CatalogEnabled;
import org.openedit.Data;

public class BaseSkill extends BaseAiManager implements Skill, CatalogEnabled
{
	public void processStarting(AgentContext inContext)
	{
		Boolean cancelStarting = (Boolean) inContext.getContextValue("cancelstartup" + inContext.getCurrentAutomationStep().getEnabledId());
		if (cancelStarting != null && cancelStarting.booleanValue())
		{
			return;
		}
		AgentJobStep skillEnabled = inContext.getCurrentAutomationStep();
		inContext.fireStatusStarting(skillEnabled);

	}

	public void processCompleted(AgentContext inContext)
	{
		// Dont run the end event if the process was skipped beause it ends when the next starts
	}

	/**
	 * This is the main process method that will be called by the agent to keep processing the children.
	 */
	@Override
	public void process(AgentContext inContext)
	{
		AgentJobStep skillEnabled = inContext.getCurrentAutomationStep();
		if( skillEnabled == null )
		{
			return;
		}

		inContext.fireStatusComplete(skillEnabled);

		Collection<AgentJobStep> children = inContext.getCurrentAutomationStep().getChildren();

		for (AgentJobStep agentEnabled : children)
		{
			AgentContext childContext = getAgentJobManager().createAgentContext(inContext, agentEnabled);

			// agentEnabled.getAgent().processstart(childContext);
			getAgentJobManager().runProcess(childContext, agentEnabled);
			// agentEnabled.getAgent().processend(childContext);
		}
	}

	public AssistantManager getAssistantManager()
	{
		AssistantManager assistantManager = (AssistantManager) getMediaArchive().getBean("assistantManager");
		return assistantManager;
	}

	protected Collection<String> loadOwnerKnowlege(String collectionid, String inUserId)
	{
		Collection<String> docids = getAssistantManager().findDocIdsForEntity("librarycollection", collectionid);

		Data ownerprofile = getAssistantManager().getEmeProfileForUser(inUserId);
		if (ownerprofile != null)
		{
			Collection<String> profiledocids = getAssistantManager().findDocIdsForEntity("emeprofile", ownerprofile.getId());
			docids.addAll(profiledocids);
		}
		return docids;
	}

	protected Collection<String> loadOwnerKnowlege(String inUserId)
	{
		Collection<String> docids = new ArrayList<>();

		Data ownerprofile = getAssistantManager().getEmeProfileForUser(inUserId);
		if (ownerprofile != null)
		{
			Collection<String> profiledocids = getAssistantManager().findDocIdsForEntity("emeprofile", ownerprofile.getId());
			docids.addAll(profiledocids);
		}
		return docids;
	}

}
