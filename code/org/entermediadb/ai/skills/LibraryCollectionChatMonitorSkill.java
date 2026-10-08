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
import org.json.simple.JSONObject;
import org.openedit.MultiValued;

public class LibraryCollectionChatMonitorSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(LibraryCollectionChatMonitorSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();
		// MultiValued currentfunction = messageContext.getCurrentFunction();

		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));
		String query = usermessage.get("message");

		String functionpath = inAgentContext.getCurrentAgentJob().getScenarioId();
		String function = inAgentContext.getCurrentAutomationStep().getEnabledId();

		functionpath = functionpath + "." + function;

		inAgentContext.put("startup_scenario", functionpath);

		// Move to its own skill, next step is parse text
		inAgentContext.put("userquery", query);

		// Collection<Data> toplevelfunctions = getMediaArchive().query("aifunction").exact("toplevel",
		// true).search();
		// inAgentContext.put("toplevelfunctions", toplevelfunctions);


		//get users who are on the chat and their roles.

		//librarycollectionusers

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");

		LlmResponse response = llmconnection.callToolsFunction(inAgentContext, "library_collection_chat_monitor");

		log.info(response.getRawResponse());
		// log.info(JsonOutput.prettyPrint(payload.toJSONString()));

		JSONObject structuredResponse = response.getToolsResponse();

		inAgentContext.put("pendinglastmessage", usermessage);
		inAgentContext.put("pendingstructuredresponse", structuredResponse);
		String tool = (String) structuredResponse.get("name");
		inAgentContext.put("possibletool", tool);

		//always Wait to respond
		response.setNextAutomationStep("chatMonitor"); // Stay in this skill?
		response.setExecAutomationStep("chatMonitorConfirmation"); // runs after the wait
		
		Map arguments = (Map) structuredResponse.get("arguments");
		Object priority = arguments != null ? arguments.get("priority_timeline") : structuredResponse.get("priority_timeline");
		int timeline = priority == null ? 0 : Math.min(5, Integer.parseInt(priority.toString()));
		inAgentContext.setWaitTime(timeline * 60L * 1000L);  //in minutes

		inAgentContext.setLastResponse(response); //Dont respond with anything yet
		//messageContext.fireStatusComplete(skillEnabled);
		return; //Show nothing

	}

	/**
	 * 
	 		inAgentContext.addContext("selectedscenario", selected_tool);

		inAgentContext.addContext("arguments", structuredResponse.get("arguments"));

		if (selected_tool.equals("showfriendlyresponse"))
		{

			llmconnection = getMediaArchive().getLlmConnection("localrender");
			response = llmconnection.renderLocalAction(inAgentContext, "chat_detect_showresponse");
			response.setNextAutomationStep("chatMonitor"); // Stay in this skill?
			inAgentContext.setLastResponse(response);

			AgentJobStep skillEnabled = messageContext.getCurrentAutomationStep();

			messageContext.fireStatusComplete(skillEnabled);

			return;
		}

	 * 
	 */
}
