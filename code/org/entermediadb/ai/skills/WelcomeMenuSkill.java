package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.agentjobs.AgentJobStep;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

/**
 * Renders a menu of the chat scenarios that sit under the same automationlabel
 * (connectedtop) as the running welcome_menu_* scenario.
 */
public class WelcomeMenuSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		MultiValued scenario = messageContext.getCurrentAgentJob().getScenarioData();
		String labelid = scenario.get("connectedtop");

		Collection<Data> menuscenarios = new ArrayList<Data>();
		if (labelid != null)
		{
			HitTracker scenarios = getMediaArchive().query("automationscenario").exact("connectedtop", labelid).exact("enabled", "true").exact("chatenabled", "true").exact("isvisible", "true").sort("ordering").search();
			for (Object object : scenarios)
			{
				Data hit = (Data) object;
				if (!hit.getId().equals(scenario.getId()))
				{
					menuscenarios.add(hit);
				}
			}
			messageContext.putContextValue("menulabel", getMediaArchive().getCachedData("automationlabel", labelid));
		}
		messageContext.putContextValue("menuscenarios", menuscenarios);




		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "welcome_menu");
		messageContext.setLastResponse(response);

		AgentJobStep skillEnabled = messageContext.getCurrentAutomationStep();
		messageContext.fireStatusComplete(skillEnabled);
		super.process(messageContext);
	}

}
