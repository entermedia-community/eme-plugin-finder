package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.openedit.MultiValued;

/**
 * Placeholder behind the EME Chatbot scenarios that are not built yet (Help Desk, Customer Onboarding, Request a Meeting, Place an Order, Lead Tracker, Order Tracker, Task Tracker,
 * Finance Tracker). Each scenario has its own bean id and aiskill row so a real skill can replace it one at a time.
 * It tells the user plainly that the feature is not implemented; it does not fake any data.
 */
public class EmeChatbotPlaceholderSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		String name = "This feature";
		ChatMessageContext messageContext = (ChatMessageContext) inContext;
		MultiValued scenario = messageContext.getCurrentAgentJob().getScenarioData();
		if (scenario != null && scenario.getName() != null)
		{
			name = scenario.getName();
		}
		BasicLlmResponse response = new BasicLlmResponse();
		response.setMessage(name + " is not implemented yet.");
		inContext.setLastResponse(response);
		super.process(inContext);
	}
}
