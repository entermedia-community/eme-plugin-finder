package org.entermediadb.ai.skills;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.llm.AutomationStep;
import org.openedit.MultiValued;

public class ChatMonitorResponseSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(ChatMonitorResponseSkill.class);

	@Override
	public void startupScenario(AgentContext inContext)
	{
		// super.startupScenario(inContext);
		// dont send hi
	}

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();

		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));

		String query = usermessage.get("message");

		// reset messagereload
		inAgentContext.putContextValue("messagereload", false);

		// TODO Fix entityid for collections?
		String entityid = inAgentContext.get("entityid");
		String selectedscenario = (String) inAgentContext.getContextValue("selectedscenario");
		if (selectedscenario == null)
		{
			log.error("No scenario selected for query: " + query);
			return;
		}
		String scenario = null;
		if (!selectedscenario.contains("."))
		{
			log.error("Selected scenario needs the format: scenario.skillenabled. Selected scenario:" + selectedscenario);
			return;
		}
		scenario = selectedscenario.split("\\.")[0];
		String skillenableid = selectedscenario.split("\\.")[1];

		log.info("Selected scenario: " + selectedscenario + " for scenario: " + scenario + " and skill: " + skillenableid);

		// we are on a task, or answering questions or another sceneration.
		if (scenario != null)
		{
			RunningScenario running = (RunningScenario) getMediaArchive().getBean("runningscenario", false);
			running.setId(scenario);

			AutomationStep skillEnabled = running.findEnabled(skillenableid);
			if (skillEnabled == null)
			{
				log.error("No skill enabled found for id: " + skillenableid);
				return;
			}
			inAgentContext.setCurrentScenario(running);
			running.runProcess(skillEnabled, inAgentContext);
		}
		else
		{
			log.error("Probem");
		}

	}

}
