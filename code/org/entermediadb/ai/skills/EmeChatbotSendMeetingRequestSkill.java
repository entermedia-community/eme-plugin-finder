package org.entermediadb.ai.skills;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.TemplateWebEmail;
import org.openedit.Data;
import org.openedit.users.User;


/**
 * Handles the form sent by EmeChatbotRequestMeetingSkill (eme_chatbot_request_meeting_submit). The form posts
 * meetingtime (yyyy-MM-dd'T'HH:mm, the visitor's local time), meetingtimezone (the visitor's UTC offset, e.g.
 * UTC+06:00) and an optional meetingmessage. Emails the vendor's owner (the chat channel's librarycollection) with the
 * visitor's name and email, then tells the visitor it was sent. Anything that stops the email (bad time, no owner or
 * owner email, a send error) is logged and the visitor is told the request was not sent.
 */
public class EmeChatbotSendMeetingRequestSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(EmeChatbotSendMeetingRequestSkill.class);

	/** plugins/community/html/default/theme/emails/ (there is no /site/profile app to fall back from) */
	protected static final String TEMPLATE = "/community/default/theme/emails/eme-chatbot-meeting-request.html";
	protected static final int MAX_MESSAGE = 1000;
	protected static final String[] KEYS = { "meetingtime", "meetingtimezone", "meetingmessage" };

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		messageContext.fireStatusStarting(messageContext.getCurrentAgentJobStep());

		String error = sendRequest(messageContext);
		for (String key : KEYS)
		{
			messageContext.put(key, null);
		}

		String action = "eme_chatbot_request_meeting_sent";
		if (error != null)
		{
			log.error("Meeting request not sent: " + error);
			action = "eme_chatbot_request_meeting_failed";
		}
		LlmConnection render = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = render.renderLocalAction(inAgentContext, action);
		messageContext.setLastResponse(response);
		super.process(messageContext);
	}

	/** Sends the email, or returns why it could not be sent */
	protected String sendRequest(ChatMessageContext inContext)
	{
		String rawtime = text(inContext, "meetingtime");
		Date time = parse(rawtime);
		if (time == null)
		{
			return "no valid meetingtime: " + rawtime;
		}
		// The time is the visitor's local time, so allow a day for time zones
		if (time.getTime() < System.currentTimeMillis() - 24L * 60 * 60 * 1000)
		{
			return "meetingtime is in the past: " + rawtime;
		}
		if (inContext.getUserProfile() == null || inContext.getUserProfile().getUser() == null)
		{
			return "no signed-in visitor";
		}
		User visitor = inContext.getUserProfile().getUser();

		MediaArchive archive = getMediaArchive();
		String vendorid = inContext.get("entityid");
		Data vendor = vendorid == null ? null : archive.getCachedData("librarycollection", vendorid);
		if (vendor == null)
		{
			return "no vendor on channel " + inContext.get("channel");
		}
		String ownerid = vendor.get("owner");
		User owner = ownerid == null ? null : archive.getUser(ownerid);
		if (owner == null)
		{
			return "vendor " + vendor.getId() + " has no owner";
		}
		if (owner.getEmail() == null || owner.getEmail().isBlank())
		{
			return "owner " + owner.getId() + " of vendor " + vendor.getId() + " has no email";
		}

		String message = text(inContext, "meetingmessage");
		if (message != null && message.length() > MAX_MESSAGE)
		{
			message = message.substring(0, MAX_MESSAGE);
		}
		String timezone = text(inContext, "meetingtimezone");
		String visitorname = visitor.getShortDescription();

		Map<String, Object> objects = new HashMap<String, Object>();
		objects.put("vendorname", html(vendor.getName()));
		objects.put("visitorname", html(visitorname));
		objects.put("visitoremail", html(visitor.getEmail()));
		objects.put("meetingtime", html(new SimpleDateFormat("EEEE d MMMM yyyy, HH:mm").format(time)));
		objects.put("meetingtimezone", html(timezone));
		objects.put("meetingmessage", message == null ? null : html(message).replace("\n", "<br/>"));

		try
		{
			TemplateWebEmail email = archive.createSystemEmail(owner, TEMPLATE);
			if (email == null)
			{
				return "could not address an email to " + owner.getEmail();
			}
			email.setSubject("Meeting request from " + visitorname);
			email.send(objects);
		}
		catch (Exception ex)
		{
			log.error("Could not email meeting request to " + owner.getEmail(), ex);
			return "send failed: " + ex.getMessage();
		}
		log.info("Meeting request from " + visitor.getEmail() + " for " + rawtime + " " + timezone + " sent to " + owner.getEmail());
		return null;
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

	/** The datetime-local input posts yyyy-MM-dd'T'HH:mm */
	protected Date parse(String inValue)
	{
		if (inValue == null)
		{
			return null;
		}
		try
		{
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			format.setLenient(false);
			return format.parse(inValue);
		}
		catch (ParseException ex)
		{
			return null;
		}
	}

	/** Visitor text goes into an HTML email */
	protected String html(String inText)
	{
		if (inText == null)
		{
			return null;
		}
		return inText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
