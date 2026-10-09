package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

/**
 * Watches a team chat. Asks library_collection_chat_monitor which menu options fit the conversation, then sleeps
 * wait_minutes on this thread. If nobody has spoken since, it renders the recommended menu. A newer message in the
 * same channel cancels the wait and is analyzed instead, so nothing is broadcast for the older one.
 */
public class LibraryCollectionChatMonitorSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(LibraryCollectionChatMonitorSkill.class);

	@Override
	public void processStarting(AgentContext inAgentContext)
	{
		//say nothing
	}

	protected Collection<Data> getMenuScenarios(Data inCurrentScenario, String inLabelId)
	{
		Collection<Data> menuscenarios = new ArrayList<Data>();
		if (inLabelId != null)
		{
			HitTracker scenarios = getMediaArchive().query("automationscenario").exact("connectedtop", inLabelId).exact("enabled", "true").exact("chatenabled", "true").exact("isvisible", "true").sort("ordering").search();
			for (Object object : scenarios)
			{
				Data hit = (Data) object;
				if (!hit.getId().equals(inCurrentScenario.getId()))
				{
					menuscenarios.add(hit);
				}
			}
		}
		return menuscenarios;
	}

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		//A newer message replaces whatever we were waiting on
		cancelPendingWait(inAgentContext);

		MultiValued usermessage = findLatestUserMessage(inAgentContext.getChannel().getId());
		if (usermessage == null)
		{
			log.info("No user message to check");
			noResponse(messageContext);
			return;
		}
		checkMessage(messageContext, usermessage);
	}

	/**
	 * Asks which menu options fit the conversation, waits wait_minutes, then renders the menu if nobody spoke since.
	 */
	protected void checkMessage(ChatMessageContext inAgentContext, MultiValued inUserMessage)
	{
		String channelid = inAgentContext.getChannel().getId();
		inAgentContext.put("userquery", inUserMessage.get("message"));

		MultiValued scenario = inAgentContext.getCurrentAgentJob().getScenarioData();
		Collection<Data> menuscenarios = getMenuScenarios(scenario, scenario.get("connectedtop"));
		inAgentContext.putContextValue("menuscenarios", menuscenarios);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
		LlmResponse response = llmconnection.callToolsFunction(inAgentContext, "library_collection_chat_monitor");
		log.info(response.getRawResponse());

		JSONObject structuredResponse = response.getToolsResponse();
		if (structuredResponse == null)
		{
			log.info("No menu recommendation for " + inUserMessage.getId());
			noResponse(inAgentContext);
			return;
		}

		Object wait = findArgument(structuredResponse, "wait_minutes");
		int minutes = wait == null ? 0 : Math.max(0, Math.min(5, Integer.parseInt(wait.toString())));

		Long milli = minutes * 60L * 1000L;

		//TODO: Divided by 10 for debuging
		if ( inAgentContext.getChannel().get("user").equals("admin"))
		{
			milli = milli / 10; // Divided by 10 for debugging
		}

		if (!waitBeforeResponding(inAgentContext, milli))
		{
			log.info("Wait cancelled for " + inUserMessage.getId() + ", a newer message is being checked");
			noResponse(inAgentContext);
			return;
		}

		MultiValued latest = findLatestUserMessage(channelid);
		if (latest != null && !latest.getId().equals(inUserMessage.getId()))
		{
			log.info("Team kept talking after " + inUserMessage.getId() + ", leaving it to " + latest.getId());
			noResponse(inAgentContext);
			return;
		}
		renderMenu(inAgentContext, structuredResponse);
	}

	protected void renderMenu(ChatMessageContext inAgentContext, Map inStructuredResponse)
	{
		Collection<Data> menuscenarios = (Collection<Data>) inAgentContext.getContextValue("menuscenarios");
		Collection<Data> recommended = new ArrayList<Data>();
		Collection<Map> scenarios = (Collection<Map>) findArgument(inStructuredResponse, "scenarios");
		if (scenarios != null && menuscenarios != null)
		{
			for (Map pick : scenarios)
			{
				Object scenarioid = pick.get("scenarioid");
				for (Data menuscenario : menuscenarios)
				{
					if (menuscenario.getId().equals(scenarioid) && !recommended.contains(menuscenario))
					{
						recommended.add(menuscenario);
					}
				}
			}
		}
		if (recommended.isEmpty())
		{
			log.info("Nothing on the menu fits, staying quiet");
			noResponse(inAgentContext);
			return;
		}
		

		inAgentContext.put("recommendedscenarios", recommended);
		inAgentContext.put("helperrole", findArgument(inStructuredResponse, "helperrole"));
		inAgentContext.put("confirmation", findArgument(inStructuredResponse, "confirmation"));
		inAgentContext.put("instruction", findArgument(inStructuredResponse, "instruction"));

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_monitor_menu");

		//Next team message comes back here
		response.setNextAutomationStep(inAgentContext.getCurrentAutomationStep().getEnabledId());
		inAgentContext.setLastResponse(response);
		inAgentContext.fireStatusComplete(inAgentContext.getCurrentAutomationStep());
	}

	protected MultiValued findLatestUserMessage(String inChannelId)
	{
		return (MultiValued) getMediaArchive().query("chatterbox").exact("channel", inChannelId).not("user", "agent").not("messagetype", "system").sort("dateDown").searchOne();
	}

	protected Object findArgument(Map inStructuredResponse, String inKey)
	{
		Object value = inStructuredResponse.get(inKey);
		if (value == null)
		{
			Map arguments = (Map) inStructuredResponse.get("arguments");
			if (arguments != null)
			{
				value = arguments.get(inKey);
			}
		}
		return value;
	}
}
