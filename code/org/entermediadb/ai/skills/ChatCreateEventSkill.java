package org.entermediadb.ai.skills;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;

/**
 * Creates the event confirmed on entity_chat_create_event_confirm. The Create Event button posts eventname,
 * eventstart, eventend, eventlocation and eventdescription. The user becomes the host and is marked as going,
 * the same as emeevent/create.json.
 */
public class ChatCreateEventSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(ChatCreateEventSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		messageContext.fireStatusStarting(messageContext.getCurrentAgentJobStep());

		String name = text(messageContext, "eventname");
		Date start = parse(text(messageContext, "eventstart"));
		Date end = parse(text(messageContext, "eventend"));
		if (name == null || start == null || (end != null && end.before(start)))
		{
			log.error("Cannot create an event without a name and a valid start before the end");
			return;
		}
		if (inAgentContext.getUserProfile() == null || inAgentContext.getUserProfile().getUser() == null)
		{
			log.error("No user to host the event");
			return;
		}
		String userid = inAgentContext.getUserProfile().getUser().getId();

		Data event = getMediaArchive().getSearcher("collectiveevent").createNewData();
		event.setName(name);
		event.setValue("eventdescription", text(messageContext, "eventdescription"));
		event.setValue("eventlocation", text(messageContext, "eventlocation"));
		event.setValue("startdate", start);
		event.setValue("enddate", end);
		event.setValue("eventhost", userid);
		event.setValue("createddate", new Date());
		getMediaArchive().saveData("collectiveevent", event);

		Data rsvp = getMediaArchive().getSearcher("collectiveeventrsvp").createNewData();
		rsvp.setValue("eventid", event.getId());
		rsvp.setValue("rsvpuser", userid);
		rsvp.setValue("rsvpstatus", "going");
		rsvp.setValue("rsvpdate", new Date());
		getMediaArchive().saveData("collectiveeventrsvp", rsvp);

		for (String key : new String[] { "eventname", "eventstart", "eventend", "eventlocation", "eventdescription" })
		{
			messageContext.putContextValue(key, null);
		}
		messageContext.putContextValue("event", event);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "entity_chat_event_created");
		messageContext.setLastResponse(response);

		super.process(messageContext);
	}

	protected String text(ChatMessageContext inContext, String inKey)
	{
		Object value = inContext.getContextValue(inKey);
		if (value == null || value.toString().isBlank())
		{
			return null;
		}
		return value.toString().trim();
	}

	/** The datetime-local input posts yyyy-MM-dd'T'HH:mm, in the server's time zone */
	protected Date parse(String inValue)
	{
		if (inValue == null)
		{
			return null;
		}
		try
		{
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(inValue);
		}
		catch (ParseException ex)
		{
			return null;
		}
	}
}
