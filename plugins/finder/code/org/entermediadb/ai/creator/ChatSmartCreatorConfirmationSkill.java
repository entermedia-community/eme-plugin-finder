package org.entermediadb.ai.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.mail.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.llm.BaseAgentContext;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.edit.Version;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.util.Inflector;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.users.User;

public class ChatSmartCreatorConfirmationSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(ChatSmartCreatorConfirmationSkill.class);

	public void process(AgentContext inContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inContext;
		String functionName = inContext.getCurrentFunctionId();
		boolean runandreturn = "chat_smartcreator_welcome".equals(functionName);
		if (functionName == null || runandreturn)
		{
			sendWelcomeMessage(messageContext);
			if (runandreturn)
			{
				return;
			}
		}
		runandreturn = "smartcreator_parse".equals(functionName);
		if (functionName == null || runandreturn)
		{
			parseResponse(messageContext, functionName); // Calls smartcreator_createoutline
			if (runandreturn)
			{
				return;
			}
		}
		runandreturn = "smartcreator_createoutline".equals(functionName);
		if (runandreturn)
		{
			super.process(inContext);
			inContext.getLastResponse().setRunFunctionName(null);
			inContext.getLastResponse().setNextFunctionName("smartcreator_confirmoutline");
			return;
		}

		runandreturn = "smartcreator_confirmoutline".equals(functionName);
		if (functionName == null || runandreturn)
		{
			confirmOutline(messageContext, functionName);
			if (runandreturn)
			{
				return;
			}
		}
		runandreturn = "smartcreator_parsecontent".equals(functionName);
		if (functionName == null || runandreturn)
		{
			parseSection(messageContext);
			if (runandreturn)
			{
				return;
			}
		}

		super.process(inContext);

	}

	public void sendWelcomeMessage(ChatMessageContext messageContext)
	{
		MultiValued currentfunction = messageContext.getCurrentFunction();

		String agentFn = currentfunction.getId();

		LlmConnection llmconnection = getMediaArchive().getLlmConnection(agentFn);
		LlmResponse response = llmconnection.renderLocalAction(messageContext, agentFn);
		// messageContext.setFunctionName("question_ask");
		messageContext.setWaitTime(null);
		messageContext.setLastResponse(response);

		return;
	}

	public void parseResponse(ChatMessageContext messageContext, String sectiontext)
	{
		Data usermessage = getMediaArchive().getCachedData("chatterbox", messageContext.getAgentMessage().get("replytoid"));

		String prompt = usermessage.get("message");

		LlmResponse response = parseCreationPrompt(messageContext, prompt);
		// TODO: Show the user what they typed and say processing

		response.setRunFunctionName("smartcreator_createoutline");
		messageContext.setLastResponse(response);
		return;
	}

	public void confirmOutline(ChatMessageContext messageContext, String agentFn)
	{
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_confirmoutline");

		AiSmartCreatorSteps instructions = messageContext.getAiSmartCreatorSteps();

		if (instructions == null)
		{
			throw new IllegalStateException("Run chat_smartcreator_suggest first");

		}

		// Adjust the outline as needed using regular AI
		messageContext.addContext("proposedoutline", instructions.getProposedSections());
		Data usermessage = getMediaArchive().getCachedData("chatterbox", messageContext.getAgentMessage().get("replytoid"));
		String prompt = usermessage.get("message");
		messageContext.addContext("confirmationprompt", prompt);

		LlmResponse res = llmconnection.callStructure(messageContext, "smartcreator_confirmoutline");

		JSONObject updatedSectionsJson = res.getMessageStructured();

		Collection<String> updatedSections = (Collection<String>) updatedSectionsJson.get("updated_outline");
		instructions.setProposedSections(updatedSections);

		boolean changed = (boolean) updatedSectionsJson.get("changed");

		messageContext.addContext("changed", changed);

		if (changed)
		{
			messageContext.addContext("proposedoutline", instructions.getProposedSections());
			LlmConnection llmconnection2 = getMediaArchive().getLlmConnection(agentFn);
			LlmResponse response = llmconnection2.renderLocalAction(messageContext, agentFn);
			response.setNextFunctionName("smartcreator_confirmoutline"); //
			messageContext.setLastResponse(response);
			return;
		}
		else
		{
			// Create Sections Content
			Data playbackentitymodule = instructions.getTargetModule();
			Data playbackentity = getMediaArchive().getSearcher(playbackentitymodule.getId()).createNewData();
			playbackentity.setValue(messageContext.get("entitymoduleid"), messageContext.get("entityid"));
			String name = instructions.getTitleName();
			name = Inflector.getInstance().capitalize(name);
			playbackentity.setName(name);
			playbackentity.setValue("entity_date", new Date());
			getMediaArchive().saveData(playbackentitymodule.getId(), playbackentity);
			instructions.setTargetEntity(playbackentity);

			initConfirmedSections(instructions);
			String step2CreatePrompt = instructions.getStepContentCreate();
			if (step2CreatePrompt != null && !step2CreatePrompt.isEmpty())
			{
				// Create the content
				BasicLlmResponse step2response = new BasicLlmResponse();
				step2response.setNextFunctionName("smartcreator_createsectioncontents");
				messageContext.setLastResponse(step2response);
			}
			else
			{
				BasicLlmResponse step2response = new BasicLlmResponse();
				step2response.setNextFunctionName("smartcreator_renderoutline");
				messageContext.setLastResponse(step2response);

				/*
				 * messageContext.addContext("confirmedoutline", instructions.getConfirmedSections());
				 * 
				 * messageContext.addContext("playbackentity", instructions.getTargetEntity());
				 * messageContext.addContext("playbackentitymodule", instructions.getTargetModule());
				 * 
				 * llmconnection = getMediaArchive().getLlmConnection("smartcreator_renderoutline"); res =
				 * llmconnection.renderLocalAction(messageContext, "smartcreator_renderoutline");
				 * messageContext.setWaitTime(null); // final interaction, no next steps. What is the next function?
				 * messageContext.setLastResponse(res);
				 */
			}
			return;
		}
	}

	public Collection<Data> initConfirmedSections(AiSmartCreatorSteps inInstructions)
	{

		MediaArchive archive = getMediaArchive();
		Searcher sectionsearcher = archive.getSearcher("componentsection");

		Collection<Data> tosave = new ArrayList<Data>();

		Collection<String> sections = inInstructions.getProposedSections();

		int ordering = 0;
		for (Iterator iterator = sections.iterator(); iterator.hasNext();)
		{
			String outline = (String) iterator.next();

			Data componentSection = sectionsearcher.createNewData();

			componentSection.setName(outline);
			componentSection.setValue("playbackentityid", inInstructions.getTargetEntity().getId());
			componentSection.setValue("playbackentitymoduleid", inInstructions.getTargetModule().getId());
			componentSection.setValue("ordering", ordering);
			componentSection.setValue("creationdate", new Date());
			componentSection.setValue("modificationdate", new Date());

			if (ordering == 0)
			{
				componentSection.setValue("contentrole", "intro");
			}
			else
				if (!iterator.hasNext())
				{
					componentSection.setValue("contentrole", "conclusion");
				}
				else
				{
					componentSection.setValue("contentrole", "body");
				}

			tosave.add(componentSection);
			ordering++;
		}

		sectionsearcher.saveAllData(tosave, null);

		inInstructions.setConfirmedSections(tosave);

		return tosave;
	}

	public LlmResponse parseCreationPrompt(AgentContext messageContext, String prompt)
	{
		// instructions come from SmartCreatorMakeSuggestionsSkill.makeSuggestions
		AiSmartCreatorSteps instructions = messageContext.getAiSmartCreatorSteps();
		messageContext.addContext("creationprompt", prompt);
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_parse");
		LlmResponse res = llmconnection.callStructure(messageContext, "smartcreator_parse");

		JSONObject paragraphs = res.getMessageStructured();
		instructions.loadJsonParts(paragraphs);

		return res;
	}

	public Collection<Map> parseSection(ChatMessageContext messageContext)
	{
		Data usermessage = getMediaArchive().getCachedData("chatterbox", messageContext.getAgentMessage().get("replytoid"));
		String sectiontext = usermessage.get("message");
		messageContext.addContext("sectiontext", sectiontext);
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_parsecontent");
		LlmResponse response = llmconnection.callStructure(messageContext, "smartcreator_parsecontent");
		JSONObject json = response.getMessageStructured();
		Collection boundaries = (Collection) json.get("parsed_content");
		return boundaries;
	}

}
