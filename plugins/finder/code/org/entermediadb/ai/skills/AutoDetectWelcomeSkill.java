package org.entermediadb.ai.skills;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;

public class AutoDetectWelcomeSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(AutoDetectWelcomeSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();
		if (messageContext.getContextValue("sentwelcome") == null)
		{
			messageContext.putContextValue("sentwelcome", true);
			agentmessage.setValue("chatmessagestatus", "completed");

			LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender"); // Should stay
			// search_start
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_detection_welcome");
			response.setNextSkillEnabled("auto_detect_conversation");
			messageContext.setLastResponse(response);
			messageContext.log("sent" + response.getMessagePlain());
		}
		// super.process(messageContext);
	}

}
