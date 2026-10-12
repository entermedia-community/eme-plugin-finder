package org.entermediadb.ai.skills;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.tasks.GoalManager;
import org.openedit.Data;
import org.openedit.MultiValued;

/**
 * Creates the goal confirmed on team_chat_create_goals_confirm. The goal is the text of a typed reply, or the
 * "goaltext" posted by the Create button.
 */
public class ChatCreateGoalSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(ChatCreateGoalSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();
		if (agentmessage == null)
		{
			return;
		}

		messageContext.fireStatusStarting(messageContext.getCurrentAutomationStep());

		//A typed reply is the goal. The Create button sends a system message with goaltext instead
		MultiValued goalmessage = null;
		String replytoid = agentmessage.get("replytoid");
		if (replytoid != null)
		{
			goalmessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", replytoid);
		}
		if (goalmessage == null || goalmessage.get("message") == null)
		{
			String goaltext = (String) messageContext.getContextValue("goaltext");
			if (goaltext == null)
			{
				goaltext = (String) messageContext.getContextValue("suggestedgoal");
			}
			if (goaltext == null || goaltext.isBlank())
			{
				log.error("No goal text to create a goal from");
				return;
			}
			agentmessage.setValue("message", goaltext);
			getMediaArchive().saveData("chatterbox", agentmessage);
			goalmessage = agentmessage;
		}
		messageContext.putContextValue("goaltext", null);
		messageContext.putContextValue("suggestedgoal", null);

		String userid = goalmessage.get("user");
		String collectionid = findCollectionId(messageContext);

		Data goal = getGoalManager().createGoal(userid, goalmessage, collectionid);
		messageContext.putContextValue("goal", goal);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_goal_created");
		messageContext.setLastResponse(response);

		//Runs team_chat_create_goals_tasks
		super.process(messageContext);
	}

	/** Team chats live on a librarycollection, a collectiveproject or a goaltask. Goals belong to the collection */
	protected String findCollectionId(ChatMessageContext inContext)
	{
		Data channel = inContext.getChannel();
		String searchtype = channel.get("searchtype");
		String entityid = inContext.get("entityid");
		if (searchtype == null || entityid == null)
		{
			return null;
		}
		if ("librarycollection".equals(searchtype))
		{
			return entityid;
		}
		Data entity = getMediaArchive().getCachedData(searchtype, entityid);
		if (entity == null)
		{
			log.error("No entity " + searchtype + " " + entityid);
			return null;
		}
		if ("collectiveproject".equals(searchtype))
		{
			return entity.get("parentcollectionid");
		}
		if ("goaltask".equals(searchtype))
		{
			return entity.get("collectionid");
		}
		return null;
	}

	private GoalManager getGoalManager()
	{
		GoalManager goalm = (GoalManager) getMediaArchive().getBean("goalManager");
		return goalm;
	}

}
