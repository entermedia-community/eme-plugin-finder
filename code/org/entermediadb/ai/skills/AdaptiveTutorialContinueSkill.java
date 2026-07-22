package org.entermediadb.ai.skills;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;

public class AdaptiveTutorialContinueSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		String tutorialid = (String) messageContext.getContextValue("tutorialid");

		String sectionid = null; // (String) messageContext.getContextValue("sectionid");
		String componentid = null; // (String) messageContext.getContextValue("componentid");

		Data topsection = null;
		Data topcomponent = null;

		Collection<Data> allsections = getMediaArchive().query("componentsection").exact("playbackentitymoduleid", "entitytutorial").exact("playbackentityid", tutorialid).sort("ordering").search();

		if (allsections.isEmpty())
		{
			endTutorial(messageContext);
			return;
		}

		if (sectionid == null)
		{
			topsection = allsections.iterator().next();
			sectionid = topsection.getId();
		}
		else
		{
			topsection = getNextData(allsections, sectionid);
			if (topsection != null)
			{
				sectionid = topsection.getId();
			}
		}

		Collection<Data> allcomponents = getMediaArchive().query("componentcontent").exact("componentsectionid", sectionid).sort("ordering").search();

		if (allcomponents.isEmpty())
		{
			endTutorial(messageContext);
			return;
			// componentid = null;
			// topsection = getNextData(allsections, sectionid);

			// if (topsection != null)
			// {
			// sectionid = topsection.getId();
			// allcomponents = getMediaArchive().query("componentcontent").exact("componentsectionid",
			// sectionid).sort("ordering").search();
			// }
		}

		if (componentid == null)
		{
			topcomponent = allcomponents.iterator().next();
		}
		else
		{
			topcomponent = getNextData(allcomponents, componentid);
		}

		if (topcomponent != null)
		{
			componentid = topcomponent.getId();
		}
		else
		{
			endTutorial(messageContext);
			return;
		}

		messageContext.putContextValue("sectionid", sectionid);
		messageContext.putContextValue("componentid", componentid);
		messageContext.putContextValue("topsection", topsection);
		messageContext.putContextValue("topcomponent", topcomponent);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(messageContext, "chat_tutor_continue");

		messageContext.setLastResponse(response);
		messageContext.log("sent" + response.getMessagePlain());

		Map<String, String> broadcastpayload = new HashMap<String, String>();
		broadcastpayload.put("messageid", topcomponent.getId());
		if ("mcq".equals(topcomponent.get("componenttype")))
		{
			broadcastpayload.put("messagetype", "question");
		}
		else if ("asset".equals(topcomponent.get("componenttype")))
		{
			broadcastpayload.put("messagetype", "asset");
		}
		else
		{
			broadcastpayload.put("messagetype", "text");
		}
		messageContext.setValue("broadcastpayload", broadcastpayload);

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);
	}

	public void endTutorial(ChatMessageContext messageContext)
	{
		AgentEnabled currentAgentEnabled = messageContext.getCurrentScenario().findEnabled("chat_tutor_end");

		messageContext.setCurrentAgentEnable(currentAgentEnabled);
		messageContext.fireStatusComplete(currentAgentEnabled);
	}

	public Data getNextData(Collection<Data> allsections, String sectionid)
	{
		Data nextsection = null;
		boolean found = false;
		for (Data section : allsections)
		{
			if (found)
			{
				nextsection = section;
				break;
			}
			if (section.getId().equals(sectionid))
			{
				found = true;
			}
		}
		return nextsection;
	}
}
