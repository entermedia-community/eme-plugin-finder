package org.entermediadb.ai.skills;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;

/**
 * Confirmation step for team_chat_create_goals. Reads the recent team messages, guesses the goal the team needs
 * and renders team_chat_create_goals_confirm so the team can edit it before team_chat_create_goals_create runs.
 */
public class ChatGuessGoalSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(ChatGuessGoalSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		String goal = null;
		String details = null;
		try
		{
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
			LlmResponse response = llmconnection.callToolsFunction(inAgentContext, "team_chat_guess_goal");
			Map structured = response.getToolsResponse();
			if (structured != null)
			{
				goal = (String) findArgument(structured, "goal");
				details = (String) findArgument(structured, "details");
			}
		}
		catch (Exception ex)
		{
			//Still show the page so the team can type the goal themselves
			log.error("Could not guess a goal", ex);
		}
		if (goal == null || goal.isBlank())
		{
			goal = (String) messageContext.getContextValue("teaminstruction");
		}
		messageContext.putContextValue("suggestedgoal", goal);
		messageContext.putContextValue("suggestedgoaldetails", details);
		//Used once, so an old team request does not steer the next guess
		messageContext.putContextValue("teaminstruction", null);

		LlmConnection render = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = render.renderLocalAction(inAgentContext, messageContext.getCurrentAgentJobStep().getEnabledId());
		messageContext.setLastResponse(response);
		//Do not run the create step yet, a typed reply or the Create button runs it
		messageContext.fireStatusComplete(messageContext.getCurrentAgentJobStep());
	}

	protected Object findArgument(Map inStructuredResponse, String inKey)
	{
		Object value = inStructuredResponse.get(inKey);
		if (value == null)
		{
			Map arguments = (Map) inStructuredResponse.get("arguments");
			if (arguments != null)
			{
				value = arguments.get(inKey);
			}
		}
		return value;
	}
}
