package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.markdown.MarkdownUtil;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.data.Searcher;

public class SmartCreatorMakeSectionsSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(SmartCreatorMakeSectionsSkill.class);

	public ChatSmartCreatorConfirmationSkill getSmartCreatorSkill()
	{
		ChatSmartCreatorConfirmationSkill smartCreatorManager = (ChatSmartCreatorConfirmationSkill) getMediaArchive().getBean("smartCreatorSkill");
		return smartCreatorManager;
	}

	@Override
	public void process(AgentContext inContext)
	{
		populateSectionsWithContents(inContext);
		super.process(inContext);
	}

	public void populateSectionsWithContents(AgentContext messageContext)
	{

		AiSmartCreatorSteps instructions = messageContext.getAiSmartCreatorSteps();

		String entityid = messageContext.getCurrentEntity().getId();
		String entitymoduleid = messageContext.getCurrentEntityModule().getId();

		AssistantManager assistant = (AssistantManager) getMediaArchive().getBean("assistantManager");

		Collection<String> parentIds = assistant.findDocIdsForEntity(entitymoduleid, entityid);
		instructions.setEmbeddedParentIds(parentIds);
		createSections(messageContext, instructions);
	}

	public void createSections(AgentContext messageContext, AiSmartCreatorSteps instructions)
	{
		Collection<Data> sections = instructions.getConfirmedSections();

		Collection<String> parentIds = instructions.getEmbeddedParentIds();

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("embedding");
		Searcher contentearcher = getMediaArchive().getSearcher("componentcontent");

		Collection<Data> tosave = new ArrayList<Data>();
		for (Iterator iterator = sections.iterator(); iterator.hasNext();)
		{
			Data section = (Data) iterator.next();
			String sectionid = section.getId();

			Map payload = new HashMap();

			String query = "For \"" + section.getName() + "\" " + instructions.getContentCreatePrompt();
			log.info("Query for section content: " + query);

			payload.put("query", query);

			payload.put("parent_ids", parentIds);

			log.info("sending to server: " + payload);

			LlmResponse res = llmconnection.callJson("/query", payload);

			JSONObject contentsJson = res.getRawResponse();

			String answer = (String) contentsJson.get("answer");

			if (answer == null)
			{
				messageContext.error("No answer found " + answer);
				continue;
			}

			log.info("Received answer for section content: " + answer);

			int ordering = 0;
			// StringBuffer exportContent = new StringBuffer();

			MarkdownUtil md = new MarkdownUtil();
			List<Map<String, String>> htmlMaps = md.getHtmlMaps(answer);

			Map<String, String> firstMap = (Map) htmlMaps.iterator().next();

			if (firstMap != null && !"Heading".equals(firstMap.get("type")))
			{
				Map<String, String> introMap = new HashMap<String, String>();
				introMap.put("type", "Heading");
				introMap.put("content", section.getName());

				htmlMaps.add(0, introMap);
			}

			for (Iterator iterator2 = htmlMaps.iterator(); iterator2.hasNext();)
			{
				Map<String, String> htmlMap = (Map) iterator2.next();

				String type = htmlMap.get("type");
				String content = htmlMap.get("content");

				String contenttype = null;

				if (type.equals("Heading"))
				{
					contenttype = "heading";
				}
				else
					if (type.equals("Paragraph") || type.equals("Text") || type.equals("HtmlBlock") || type.contains("List"))
					{
						contenttype = "paragraph";
					}
					else
					{
						log.info("Unknown content type: " + type);
						log.info("Content: " + content);
						continue;
					}

				// exportContent.append("<strong>" + contenttype + "</strong>\n");
				// exportContent.append("<div>" + content + "</div>\n\n");

				Data componentcontent = contentearcher.createNewData();
				componentcontent.setValue("componentsectionid", sectionid);
				componentcontent.setValue("content", content);
				componentcontent.setValue("componenttype", contenttype);
				componentcontent.setValue("ordering", ordering);
				componentcontent.setValue("creationdate", new Date());
				componentcontent.setValue("modificationdate", new Date());

				tosave.add(componentcontent);

				ordering++;
			}

			// messageContext.addContext("output", exportContent.toString());

			// exportAsAsset(messageContext, exportContent.toString());

			/*
			 * Improve speed // try semantically matching an asset to the section SearchingSkill
			 * searchingmanager = (SearchingSkill) getMediaArchive().getBean("searchingSkill"); String
			 * playbackentityid = section.get("playbackentityid"); String playbackentitymoduleid =
			 * section.get("playbackentitymoduleid"); Data playbackentity =
			 * getMediaArchive().getCachedData(playbackentitymoduleid, playbackentityid); String creatorName =
			 * playbackentity.getName(); String sectionName = section.getName();
			 * 
			 * Data asset = searchingmanager.semanticSearchBestMatch(creatorName + " " + sectionName, "asset");
			 * 
			 * if(asset != null) { Data componentcontent = contentearcher.createNewData();
			 * componentcontent.setValue("componentsectionid", sectionid); componentcontent.setValue("assetid",
			 * asset.getId()); String caption = asset.get("headline"); if(caption == null) { caption =
			 * asset.get("longcaption"); } if(caption != null) { componentcontent.setValue("content", caption);
			 * } componentcontent.setValue("componenttype", "asset"); componentcontent.setValue("ordering",
			 * ordering); componentcontent.setValue("creationdate", new Date());
			 * componentcontent.setValue("modificationdate", new Date()); }
			 */

			if (tosave.size() >= 5)
			{
				contentearcher.saveAllData(tosave, null);
				tosave.clear();
			}

		}
		if (!tosave.isEmpty())
		{
			contentearcher.saveAllData(tosave, null);
		}
	}
}
