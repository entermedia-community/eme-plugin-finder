package org.entermediadb.ai.skills;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;

/**
 * Confirmation step for entity_chat_create_event. Reads what the user asked for, drafts the event and renders
 * entity_chat_create_event_confirm so the user can edit it before entity_chat_create_event_create runs.
 */
public class ChatGuessEventSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(ChatGuessEventSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		//The tools function resolves words like "next Friday" from this
		messageContext.putContextValue("currentdate", new SimpleDateFormat("EEEE yyyy-MM-dd'T'HH:mm").format(new Date()));

		String name = null;
		String start = null;
		String end = null;
		String location = null;
		String description = null;
		try
		{
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
			LlmResponse response = llmconnection.callToolsFunction(inAgentContext, "entity_chat_guess_event");
			Map structured = response.getToolsResponse();
			if (structured != null)
			{
				name = (String) findArgument(structured, "name");
				start = (String) findArgument(structured, "start");
				end = (String) findArgument(structured, "end");
				location = (String) findArgument(structured, "location");
				description = (String) findArgument(structured, "description");
			}
		}
		catch (Exception ex)
		{
			//Still show the form so the user can type the event themselves
			log.error("Could not draft an event", ex);
		}
		messageContext.putContextValue("suggestedeventname", name);
		messageContext.putContextValue("suggestedeventstart", start);
		messageContext.putContextValue("suggestedeventend", end);
		messageContext.putContextValue("suggestedeventlocation", location);
		messageContext.putContextValue("suggestedeventdescription", description);

		LlmConnection render = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = render.renderLocalAction(inAgentContext, messageContext.getCurrentAgentJobStep().getEnabledId());
		messageContext.setLastResponse(response);
		//Do not run the create step yet, the Create Event button runs it
		messageContext.fireStatusComplete(messageContext.getCurrentAgentJobStep());
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
