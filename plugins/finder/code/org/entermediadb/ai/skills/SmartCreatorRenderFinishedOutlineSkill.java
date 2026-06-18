package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;

public class SmartCreatorRenderFinishedOutlineSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		String functionName = inContext.getCurrentAgentEnable().getEnabledId();
		boolean runandreturn = "smartcreator_renderoutline".equals(functionName);
		if (functionName == null || runandreturn)
		{
			renderFinishedOutline(inContext, inContext.getAiSmartCreatorSteps());
			if (runandreturn)
			{
				return;
			}
		}

		super.process(inContext);
	}

	public void renderFinishedOutline(AgentContext messageContext, AiSmartCreatorSteps instructions)
	{
		messageContext.addContext("confirmedoutline", instructions.getConfirmedSections());
		messageContext.addContext("playbackentity", instructions.getTargetEntity());
		messageContext.addContext("playbackentitymodule", instructions.getTargetModule());
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(messageContext, "smartcreator_renderoutline");
		messageContext.setWaitTime(null);
		messageContext.setLastResponse(response);
	}

}
