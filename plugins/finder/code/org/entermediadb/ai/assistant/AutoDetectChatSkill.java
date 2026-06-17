package org.entermediadb.ai.assistant;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

public class AutoDetectChatSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(AutoDetectChatSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();
		MultiValued currentfunction = messageContext.getCurrentFunction();
		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));
		String query = usermessage.get("message");

		String agentFn = currentfunction.getId();

		if ("chat_detection_welcome".equals(agentFn) || agentFn == null)
		{
			if (messageContext.getContextValue("sentwelcome") != null)
			{
				return;
			}

			messageContext.putContextValue("sentwelcome", true);
			agentmessage.setValue("chatmessagestatus", "completed");

			LlmConnection llmconnection = getMediaArchive().getLlmConnection(agentFn); // Should stay
			// search_start
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, agentFn);
			response.setNextFunctionName("auto_detect_conversation");
			messageContext.setLastResponse(response);
			messageContext.log("sent" + response.getMessagePlain());
			return;
		}
		else
			if ("auto_detect_conversation".equals(agentFn)) // Todo: Rename to Parse
			{

				inAgentContext.put("userquery", query);

				Collection<Data> toplevelfunctions = getMediaArchive().query("aifunction").exact("toplevel", true).search();
				inAgentContext.put("toplevelfunctions", toplevelfunctions);

				LlmConnection llmconnection = getMediaArchive().getLlmConnection(agentFn);

				LlmResponse response = llmconnection.callToolsFunction(inAgentContext, agentFn);

				log.info(response.getRawResponse());

				String functionName = response.getRunFunctionName();
				JSONObject functionArgs = response.getFunctionArguments();

				inAgentContext.addContext("messagestructured", response.getMessageStructured());
				inAgentContext.addContext("userquery", query);
				inAgentContext.addContext("arguments", functionArgs);
				response.setRunFunctionName(functionName);

				/*
				 * // TODO: sync with auto created function names if("create_tutorial".equals(functionName)) {
				 * inAgentContext.addContext("playbackentitymoduleid", "aitutorial");
				 * inAgentContext.setTopLevelFunctionName("welcome_aitutorials");
				 * inAgentContext.setFunctionName("welcome_aitutorials");
				 * inAgentContext.setRunFunctionName("create_aitutorials"); } else
				 * if("play_tutorial".equals(functionName)) { inAgentContext.addContext("playbackentitymoduleid",
				 * "aitutorial"); inAgentContext.setTopLevelFunctionName("welcome_aitutorials");
				 * inAgentContext.setFunctionName("play_tutorial");
				 * inAgentContext.setRunFunctionName("play_tutorial"); } else
				 * if("image_creation".equals(functionName)) {
				 * inAgentContext.setTopLevelFunctionName("welcomeQuestions");
				 * inAgentContext.setFunctionName("welcomeQuestions");
				 * inAgentContext.setRunFunctionName("welcomeQuestions"); } else {
				 * inAgentContext.setFunctionName("auto_detect_conversation"); }
				 */
				messageContext.setLastResponse(response);
				return;
			}
			else
				if ("auto_detect_showresponse".equals(agentFn))
				{
					LlmConnection llmconnection = getMediaArchive().getLlmConnection(agentFn); // Should stay search_start
					LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "auto_detect_showresponse");
					response.setNextFunctionName("auto_detect_conversation");
					messageContext.setLastResponse(response);
					return;
				}
				else
					if ("auto_detect_sitewide_welcome".equals(agentFn))
					{
						agentmessage.setValue("chatmessagestatus", "completed");

						LlmConnection llmconnection = getMediaArchive().getLlmConnection(agentFn); // Should stay search_start
						LlmResponse response = llmconnection.renderLocalAction(inAgentContext, agentFn);
						response.setNextFunctionName("auto_detect_sitewide_parse");
						messageContext.setLastResponse(response);
						return;
					}
					else
						if ("auto_detect_sitewide_parse".equals(agentFn))
						{
							LlmConnection llmconnection = getMediaArchive().getLlmConnection(currentfunction.getId()); // Should stay
							// search_start
							LlmResponse response = llmconnection.callToolsFunction(inAgentContext, agentFn);

							log.info(response.getRawResponse());

							String functionName = response.getRunFunctionName();
							JSONObject functionArgs = response.getFunctionArguments();
							inAgentContext.addContext("arguments", functionArgs);
							response.setRunFunctionName(functionName);
							inAgentContext.setLastResponse(response);
						}

		throw new OpenEditException("Function not supported " + agentFn);

	}

}
