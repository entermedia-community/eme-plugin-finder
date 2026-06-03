package org.entermediadb.ai.creator.agents;

import java.util.Collection;

import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.creator.SmartCreatorSkill;
import org.entermediadb.ai.AgentContext;
import org.openedit.Data;

public class SmartCreatorCreateOutlineSkill extends BaseSkill
{

	public SmartCreatorSkill getSmartCreatorSkill()
	{
		SmartCreatorSkill smartCreatorManager = (SmartCreatorSkill) getMediaArchive().getBean("smartCreatorSkill");
		return smartCreatorManager;
	}

	@Override
	public void process(AgentContext inContext)
	{
		Data module = inContext.getCurrentEntityModule();
		Data entity = inContext.getCurrentEntity();

		AssistantManager assistant = (AssistantManager) getMediaArchive().getBean("assistantManager");
		getSmartCreatorSkill().createOutLine(inContext, inContext.getAiSmartCreatorSteps());
		getSmartCreatorSkill().initConfirmedSections(inContext.getAiSmartCreatorSteps());

		super.process(inContext);

	}
}
