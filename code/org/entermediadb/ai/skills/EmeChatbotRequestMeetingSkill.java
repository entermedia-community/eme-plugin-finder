package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;

/**
 * EME Chatbot "Request a Meeting" (eme_chatbot_request_meeting_confirm): sends the visitor a form with a date and time
 * and an optional short message. Its submit button runs eme_chatbot_request_meeting_submit
 * (EmeChatbotSendMeetingRequestSkill).
 */
public class EmeChatbotRequestMeetingSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		LlmConnection render = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = render.renderLocalAction(inAgentContext, "eme_chatbot_request_meeting_confirm");
		messageContext.setLastResponse(response);
		// Do not run the submit step yet, the form's button runs it
		messageContext.fireStatusComplete(messageContext.getCurrentAgentJobStep());
	}
}
