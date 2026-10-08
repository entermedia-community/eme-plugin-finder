package org.entermediadb.ai.skills;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.agentjobs.AgentJobStep;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;

/**
 * Runs after LibraryCollectionChatMonitorSkill has waited priority_timeline minutes. If nobody has said anything since
 * the message that was analyzed, send the confirmation from library_collection_chat_monitor back to the team. Otherwise stay
 * quiet, the newer message has its own monitor run.
 */
public class ChatMonitorConfirmationSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(ChatMonitorConfirmationSkill.class);

	@Override
	public void processStarting(AgentContext inContext)
	{
		// dont send status until we know we will respond
	}

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		MultiValued pendinglastmessage = (MultiValued) inAgentContext.getContextValue("pendinglastmessage");
		if (pendinglastmessage == null)
		{
			log.info("No pending message, skipping confirmation");
			return;
		}
		if (hasNewerMessage(inAgentContext.getChannel().getId(), pendinglastmessage.getId()))
		{
			log.info("Team kept talking after " + pendinglastmessage.getId() + ", skipping confirmation");
			return;
		}

		String confirmation = (String) findArgument(inAgentContext, "confirmation");
		if (confirmation == null || confirmation.isEmpty())
		{
			log.info("No confirmation to send for " + pendinglastmessage.getId());
			return;
		}

		inAgentContext.put("confirmation", confirmation);
		inAgentContext.put("role", findArgument(inAgentContext, "role"));
		inAgentContext.put("instruction", findArgument(inAgentContext, "instruction"));

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_monitor_confirmation");

		//Next team message goes back to the monitor
		response.setNextAutomationStep("chatMonitor");
		inAgentContext.setLastResponse(response);

		AgentJobStep skillEnabled = messageContext.getCurrentAutomationStep();
		messageContext.fireStatusComplete(skillEnabled);
	}

	protected boolean hasNewerMessage(String inChannelId, String inReplyToId)
	{
		MultiValued latest = (MultiValued) getMediaArchive().query("chatterbox").exact("channel", inChannelId).not("user", "agent").not("messagetype", "system").sort("dateDown").searchOne();
		return latest != null && !latest.getId().equals(inReplyToId);
	}

	protected Object findArgument(AgentContext inAgentContext, String inKey)
	{
		Map pending = (Map) inAgentContext.getContextValue("pendingstructuredresponse");
		if (pending == null)
		{
			return null;
		}
		Object value = pending.get(inKey);
		if (value == null)
		{
			Map arguments = (Map) pending.get("arguments");
			if (arguments != null)
			{
				value = arguments.get(inKey);
			}
		}
		return value;
	}

}
