package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;
import org.openedit.MultiValued;

public class SmartCreatorMakeSuggestionsSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		String functionName = inContext.getCurrentAgentEnable().getEnabledId();
		boolean runandreturn = "chat_smartcreator_suggest".equals(functionName);
		if (functionName == null || runandreturn)
		{
			makeSuggestions(inContext, inContext.getAiSmartCreatorSteps(), functionName);
			if (runandreturn)
			{
				return;
			}
		}

		super.process(inContext);
	}

	public void makeSuggestions(AgentContext messageContext, AiSmartCreatorSteps instructions, String agentFn)
	{

		// This was passed in from html data-context_playbackentitymoduleid="$module.getId()"
		String playbackentitymoduleid = (String) messageContext.getContextValue("playbackentitymoduleid");
		Data playbackentitymodule = getMediaArchive().getCachedData("module", playbackentitymoduleid);
		messageContext.addContext("playbackentitymodule", playbackentitymodule);

		instructions = new AiSmartCreatorSteps();
		instructions.setTargetModule(playbackentitymodule);
		messageContext.setAiSmartCreatorSteps(instructions);

		// oldway?
		String entityid = messageContext.get("entityid");
		String entitymoduleid = messageContext.get("entitymoduleid");
		// --

		MultiValued entity = (MultiValued) getMediaArchive().getCachedData(entitymoduleid, entityid);
		messageContext.addContext("entity", entity);

		messageContext.setCurrentEntity(entity);

		MultiValued module = (MultiValued) getMediaArchive().getCachedData("module", entitymoduleid);
		messageContext.setCurrentEntityModule(module);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
		LlmResponse response = llmconnection.renderLocalAction(messageContext, agentFn);
		messageContext.setWaitTime(null);
		// This is for the chat UI to pass it back
		response.setNextSkillEnabled("smartcreator_parse");
		messageContext.setLastResponse(response);
		return;
	}

}
