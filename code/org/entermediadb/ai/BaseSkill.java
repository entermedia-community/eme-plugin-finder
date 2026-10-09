package org.entermediadb.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.agentjobs.AgentJobStep;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.CatalogEnabled;
import org.openedit.Data;

public class BaseSkill extends BaseAiManager implements Skill, CatalogEnabled
{
	private static final Log log = LogFactory.getLog(BaseSkill.class);

	/** One pending wait per channel, so a newer message in that channel can cancel it */
	protected static final Map<String, PendingWait> fieldPendingWaits = new ConcurrentHashMap<String, PendingWait>();

	protected static class PendingWait
	{
		protected boolean fieldCancelled;

		public synchronized void cancel()
		{
			fieldCancelled = true;
			notifyAll();
		}

		/** @return true if the full wait passed without being cancelled */
		public synchronized boolean await(long inMillis) throws InterruptedException
		{
			long end = System.currentTimeMillis() + inMillis;
			long remaining = inMillis;
			while (!fieldCancelled && remaining > 0)
			{
				wait(remaining);
				remaining = end - System.currentTimeMillis();
			}
			return !fieldCancelled;
		}
	}

	/** Waits are per skill and channel. Contexts without a channel cannot be cancelled */
	protected String getWaitKey(AgentContext inContext)
	{
		if (inContext.getChannel() == null)
		{
			return null;
		}
		return getClass().getName() + "/" + inContext.getChannel().getId();
	}

	/** Wakes up this skill if it is waiting on the same channel, so it gives up without responding */
	protected void cancelPendingWait(AgentContext inContext)
	{
		String key = getWaitKey(inContext);
		if (key == null)
		{
			return;
		}
		PendingWait old = fieldPendingWaits.remove(key);
		if (old != null)
		{
			old.cancel();
		}
	}

	/**
	 * Sleeps on this thread before responding. Call processUpdate first to show progress while waiting.
	 * @return true if the wait finished, false if cancelPendingWait was called for the same skill and channel
	 */
	protected boolean waitBeforeResponding(AgentContext inContext, long inMillis)
	{
		if (inMillis <= 0)
		{
			return true;
		}
		String key = getWaitKey(inContext);
		PendingWait pending = new PendingWait();
		if (key != null)
		{
			PendingWait old = fieldPendingWaits.put(key, pending);
			if (old != null)
			{
				old.cancel();
			}
		}
		try
		{
			
			log.info("Waiting " + inMillis / 1000L + " seconds before responding on " + key);
			return pending.await(inMillis);
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			return false;
		}
		finally
		{
			if (key != null)
			{
				fieldPendingWaits.remove(key, pending);
			}
		}
	}

	/** Ends this step without broadcasting anything */
	protected void noResponse(AgentContext inContext)
	{
		LlmResponse quiet = new BasicLlmResponse();
		quiet.setOperationState("cancel");
		inContext.setLastResponse(quiet);
	}

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

	public void processUpdate(AgentContext inContext)
	{
		AgentJobStep skillEnabled = inContext.getCurrentAutomationStep();
		if (skillEnabled != null)
		{
			inContext.fireStatusUpdate(skillEnabled);
		}
	}

	public void processCompleted(AgentContext inContext)
	{
		AgentJobStep skillEnabled = inContext.getCurrentAutomationStep();
		if (skillEnabled != null)
		{
			inContext.fireStatusComplete(skillEnabled);
		}
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

		processCompleted(inContext);

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
