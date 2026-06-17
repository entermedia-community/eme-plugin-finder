package org.entermediadb.ai.creator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;

public class SmartCreatorCreateOutlineSkill extends BaseSkill
{

	public ChatSmartCreatorConfirmationSkill getSmartCreatorSkill()
	{
		ChatSmartCreatorConfirmationSkill smartCreatorManager = (ChatSmartCreatorConfirmationSkill) getMediaArchive().getBean("smartCreatorSkill");
		return smartCreatorManager;
	}

	@Override
	public void process(AgentContext inContext)
	{
		createOutLine(inContext, inContext.getAiSmartCreatorSteps());
		super.process(inContext);

	}

	// instructions come from SmartCreatorMakeSuggestionsSkill.makeSuggestions
	public void createOutLine(AgentContext messageContext, AiSmartCreatorSteps instructions)
	{

		Collection<String> parentIds = instructions.getEmbeddedParentIds();

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_createoutline");
		Map payload = new HashMap();
		payload.put("query", instructions.getOutlineCreatePrompt());

		payload.put("parent_ids", parentIds);
		// log.info("Sending: " + payload);
		LlmResponse response = llmconnection.callJson("/create_outline", payload);

		JSONObject outlineJson = response.getRawResponse();

		Collection<String> outline = (Collection<String>) outlineJson.get("outline");

		Collection<String> cleanedOutline = new ArrayList<String>();

		for (Iterator iterator = outline.iterator(); iterator.hasNext();)
		{
			String section = (String) iterator.next();
			section = section.replaceAll("^\\s+", "");
			section = section.replaceAll("\\s+$", "");
			section = section.replaceFirst("^\\d+\\.\\s+", "");
			section = section.replaceFirst("^[A-Za-z].\\s+", "");
			section = section.replaceFirst("^[IVX]+\\.\\s+", "");

			cleanedOutline.add(section);
		}

		instructions.setProposedSections(cleanedOutline);
		messageContext.addContext("proposedoutline", instructions.getProposedSections());

		if (messageContext.getContextValue("confirmoutline") != null)
		{
			messageContext.getLastResponse().setRunSkillEnabled(null);
			messageContext.getLastResponse().setNextSkillEnabled("smartcreator_confirmoutline");
		}

	}

}
