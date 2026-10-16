package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.ai.assistant.AiSearchTable;
import org.entermediadb.ai.classify.SemanticClassifierManager;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;

public class SearchingSemanticSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(SearchingSemanticSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;


		String semanticquery = "";

		AiSearch searchParams = inAgentContext.getAiSearchParams();
		AiSearchTable step1 = searchParams.getStep1();

		String step1Keyword = step1.getParameterValues();

		if (step1Keyword != null)
		{
			semanticquery = step1Keyword;
		}
		AiSearchTable step2 = searchParams.getStep2();
		if (step2 != null)
		{
			String step2Keyword = step2.getParameterValues();
			if (step2Keyword != null)
			{
				semanticquery = semanticquery + " " + step2Keyword;
			}
		}
		if (semanticquery.isBlank())
		{
			String originalQuery = searchParams.getOriginalSearchString();
			if (originalQuery != null)
			{
				originalQuery = originalQuery.replaceAll("search|find|look for|show me", "").trim();
				semanticquery = originalQuery;
			}
		}
		LlmResponse result = null;
		if (!semanticquery.isBlank())
		{
			inAgentContext.addContext("semanticquery", semanticquery.trim());

			LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
			result = llmconnection.renderLocalAction(inAgentContext, "search_semantic");
		}
		else
		{
			String messageprefix = inAgentContext.getMessagePrefix();
			result = new BasicLlmResponse();
			result.setRawMessage(messageprefix);

		}
		inAgentContext.setMessagePrefix(null);
		// Set next function to be able to search again
		result.setNextAutomationStep("view_related_records");
		messageContext.setLastResponse(result);
		messageContext.fireStatusComplete(messageContext.getCurrentAgentJobStep());
		return;

	}

	public void semanticSearch(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive();

		String semanticquery = inReq.getRequestParameter("semanticquery");

		Collection<String> excludeEntityIds = inReq.getRequestCollection("excludeentityids");
		Collection<String> excludeAssetIds = inReq.getRequestCollection("excludeassetids");

		log.info("Semantic Search for: " + semanticquery);
		inReq.putPageValue("input", semanticquery);

		Map<String, Collection<String>> relatedEntityIds = getSemanticTopicManager().search(semanticquery, excludeEntityIds, excludeAssetIds);

		log.info("Related Entity Ids: " + relatedEntityIds);

		Collection<Data> semanticentities = new ArrayList();
		Map<String, HitTracker> semanticentityhits = new HashMap();
		HitTracker semanticassethits = null;

		for (Iterator iterator = relatedEntityIds.keySet().iterator(); iterator.hasNext();)
		{
			String moduleid = (String) iterator.next();

			Data module = archive.getCachedData("module", moduleid);

			Collection<String> ids = relatedEntityIds.get(moduleid);

			HitTracker entites = getMediaArchive().query(moduleid).ids(ids).search();

			if (entites == null || entites.size() == 0)
			{
				continue;
			}

			semanticentities.add(module);

			if (moduleid.equals("asset"))
			{
				semanticassethits = entites;
			}
			else
			{
				semanticentityhits.put(moduleid, entites);
			}
		}

		inReq.putPageValue("semanticentities", semanticentities);
		inReq.putPageValue("semanticentityhits", semanticentityhits);
		if (semanticassethits == null)
		{
			inReq.putPageValue("semanticassethits", new ArrayList());
		}
		else
		{
			inReq.putPageValue("semanticassethits", semanticassethits);
		}
	}

	public Data semanticSearchBestMatch(String inQuery, String inModuleId)
	{
		Data relatedEntityId = getSemanticTopicManager().searchOne(inQuery, inModuleId);
		return relatedEntityId;
	}

	protected SemanticClassifierManager fieldSemanticTopicManager;

	public SemanticClassifierManager getSemanticTopicManager()
	{
		if (fieldSemanticTopicManager == null)
		{
			fieldSemanticTopicManager = (SemanticClassifierManager) getModuleManager().getBean(getCatalogId(), "semanticClassifierManager", false);
			fieldSemanticTopicManager.setConfigurationId("semantictopics");
		}

		return fieldSemanticTopicManager;
	}

	public Map<String, Collection<String>> searchRelatedEntitiesBySearchCategory(MultiValued searchcategory)
	{
		if (searchcategory.getBoolean("semanticindexed"))
		{
			// Todo: Use the Vector DB?
		}
		Collection values = searchcategory.getValues("semantictopics");
		Map<String, Collection<String>> results = getSemanticTopicManager().search(values, null, null);
		return results;
	}

	public void rescanSearchCategories(ScriptLogger inLogger)
	{
		// For each search category go look for relevent records. Reset old ones?
		HitTracker tracker = getMediaArchive().query("searchcategory").exists("semantictopics").search();
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			MultiValued searchcategory = (MultiValued) iterator.next();
			Collection sumatics = searchcategory.getValues("semantictopics");
			Map<String, Collection<String>> bytype = searchRelatedEntitiesBySearchCategory(searchcategory);

			for (Iterator iterator2 = bytype.keySet().iterator(); iterator2.hasNext();)
			{
				String moduleid = (String) iterator2.next();
				if ("searchcategory".equals(moduleid))
				{
					continue;
				}
				Collection<String> ids = bytype.get(moduleid);
				Collection addedentites = getMediaArchive().query(moduleid).ids(ids).not("searchcategory", searchcategory.getId()).search();
				if (ids.size() != addedentites.size())
				{
					// log.info("Mistmatch, missing records, recreate Semantics. " + ids.size() + "
					// != "+ addedentites.size());
				}
				// Collection addedentites =
				// getMediaArchive().query(moduleid).ids(ids).search();

				inLogger.info("Found " + ids.size() + " " + moduleid + " for Category: " + searchcategory);

				Collection tosave = new ArrayList(addedentites.size());
				for (Iterator iterator3 = addedentites.iterator(); iterator3.hasNext();)
				{
					MultiValued entity = (MultiValued) iterator3.next();
					entity.addValue("searchcategory", searchcategory.getId());
					tosave.add(entity);
				}
				if (tosave.size() > 0)
				{
					getMediaArchive().saveData(moduleid, tosave);
				}
				inLogger.info("Added " + tosave.size() + " out of " + ids.size());
			}
		}
	}
}
