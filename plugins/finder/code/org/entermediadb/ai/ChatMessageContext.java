package org.entermediadb.ai;

import org.entermediadb.ai.llm.BaseAgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;

public class ChatMessageContext extends BaseAgentContext
{

	public ChatMessageContext(AgentContext inContext) {
		super(inContext);
	}

	public ChatMessageContext() {
		// TODO Auto-generated constructor stub
	}

	public MultiValued getAgentMessage()
	{
		return (MultiValued) getContextValue("agentmessage");
	}

	public void setAgentMessage(MultiValued inMessage)
	{
		putContextValue("agentmessage", inMessage);
	}

	public MultiValued getUserMessage()
	{
		return (MultiValued) getContextValue("usermessage");
	}

	public void setUserMessage(MultiValued inMessage)
	{
		putContextValue("usermessage", inMessage);
	}

	public MultiValued getAiFunction()
	{
		return (MultiValued) getContextValue("aiFunction");
	}

	public void setAiFunction(MultiValued inAiFunction)
	{
		putContextValue("aiFunction", inAiFunction);
	}

	public LlmResponse getLastResponse()
	{
		return (LlmResponse) getContextValue("lastResponse");
	}

	public void setLastResponse(LlmResponse inLastResponse)
	{
		putContextValue("lastResponse", inLastResponse);
	}
}
