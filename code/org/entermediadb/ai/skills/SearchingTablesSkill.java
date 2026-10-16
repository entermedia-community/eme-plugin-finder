package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.Schema;
import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.ai.assistant.AiSearchTable;
import org.entermediadb.ai.assistant.SemanticAction;
import org.entermediadb.ai.classify.SemanticTableManager;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.find.ResultsManager;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;

public class SearchingTablesSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(SearchingTablesSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		String agentFn = messageContext.getCurrentAgentJobStep().getEnabledId();

		// Steps are named <scenario>_welcome, _parse, _tables and _semantic so more than one scenario can
		// search
		String prefix = "";
		if (messageContext.getCurrentAgentJob() != null)
		{
			prefix = messageContext.getCurrentAgentJob().getScenarioId() + "_";
		}
		String action = agentFn.startsWith(prefix) ? agentFn.substring(prefix.length()) : agentFn;

		if ("tables".equals(action))
		{
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
			LlmResponse response = llmconnection.renderLocalAction(messageContext, "search_tables"); // TOREVIEW: This is rendering
			// previous searches results first

			String message = response.getMessage();
			messageContext.setMessagePrefix(message);

			response.setExecAutomationStep(prefix + "semantic");

			messageContext.setLastResponse(response);
			
			return;
		}

		throw new OpenEditException("Function not supported " + agentFn);
	}

	public void searchTables(WebPageRequest inReq, AiSearch inAiSearchParams)
	{
		AiSearchTable step1 = inAiSearchParams.getStep1();
		AiSearchTable step2 = inAiSearchParams.getStep2();
		// AiSearchPart part3 = inAiSearchParams.getPart3();

		HitTracker entityhits = null;
		HitTracker assethits = null;

		String step1Keyword = null;
		String step2Keyword = null;
		// See if we should process
		if (step1 != null && step2 != null)
		{
			step1Keyword = step1.getParameterValues();

			HitTracker foundhits = getMediaArchive().query(step1.getModule().getId()).freeform("description", step1Keyword).search();

			if (foundhits.isEmpty())
			{
				return;
			}
			Collection<String> ids = foundhits.collectValues("id");

			inReq.putPageValue("module", step2.getModule());

			QueryBuilder search = getMediaArchive().query(step2.getModule().getId()).named("assitedsearch").orgroup(step1.getModule().getId(), ids);
			step2Keyword = step2.getParameterValues();
			if (step2Keyword != null)
			{
				if (step1Keyword == null || !step1Keyword.equalsIgnoreCase(step2Keyword)) // Remove the keyword if is
																							// same
				{
					// Remove Module Name from Keywords
					/*
					 * String step1table = step1.getTargetTable(); step2Keyowrd = step2Keyowrd.replace(step1table, "");
					 * if (step1table.endsWith("s")) { step1table = step1table.substring(0, step1table.length()-1);
					 * step2Keyowrd = step2Keyowrd.replace(step1table, ""); }
					 */
					search.freeform("description", step2Keyword);
				}
			}
			entityhits = search.search();

			step2.setCount((long) entityhits.size());

			// inReq.putPageValue( finalhits.getSessionId(), finalhits);

		}
		else if (step1 != null)
		{

			step1Keyword = step1.getParameterValues();

			inReq.putPageValue("module", step1.getModule());
			if (step1Keyword != null)
			{
				QueryBuilder search = null;

				Schema schema = loadSchema();
				Collection<Data> modules = schema.getChildrenOf(step1.getModule().getId());
				Collection<String> moduleids = new ArrayList<String>();
				moduleids.add(step1.getModule().getId());
				// step1.addModule(step1.getModule());
				for (Iterator iterator = modules.iterator(); iterator.hasNext();)
				{
					Data mod = (Data) iterator.next();
					moduleids.add(mod.getId());
					step1.addModule(mod);
				}

				if (moduleids.contains("modulesearch") || moduleids.size() > 1)
				{

					if (moduleids.size() == 1 || moduleids.contains("modulesearch"))
					{
						moduleids = schema.getModuleIds();
					}

					search = getMediaArchive().query("modulesearch").addFacet("entitysourcetype").put("searchtypes", moduleids).includeDescription(true);
					entityhits = search.freeform("description", step1Keyword).search();
					log.info(" Here:  " + entityhits.getActiveFilterValues());
					// TODO: Not sure this is needed or works
					if (moduleids.contains("asset"))
					{
						// Loop over the categories in these entityhits
						Collection categories = entityhits.collectValues("rootcategory");
						search = getMediaArchive().query("asset").orgroup("category", categories);
						assethits = search.freeform("description", step1Keyword).search();
						step1.setCount((long) (entityhits.size() + assethits.size()));
					}
					else
					{
						step1.setCount((long) entityhits.size());
					}

				}
				else if ("asset".equals(step1.getModule().getId()))
				{
					search = getMediaArchive().query(step1.getModule().getId());
					assethits = search.freeform("description", step1Keyword).search();
					step1.setCount((long) assethits.size());
				}
				else
				{
					search = getMediaArchive().query(step1.getModule().getId());
					entityhits = search.freeform("description", step1Keyword).search();
					step1.setCount((long) entityhits.size());
				}
			}
			else
			{
				// step1.addModule(step1.getModule());
				entityhits = getMediaArchive().query(step1.getModule().getId()).all().search();
				step1.setCount((long) entityhits.size());
			}

		}

		if (entityhits != null)
		{
			inReq.putPageValue("hits", entityhits);
		}
		else
		{
			inReq.putPageValue("hits", assethits);
		}

		if (entityhits != null || assethits != null)
		{
			organizeResults(inReq, entityhits, assethits);
		}

	}

	public ResultsManager getResultsManager()
	{
		ResultsManager resultsManager = (ResultsManager) getMediaArchive().getBean("resultsManager");
		return resultsManager;
	}

	public void organizeResults(WebPageRequest inReq, HitTracker entityhits, HitTracker assetunsorted)
	{

		inReq.putPageValue("assethits", assetunsorted);

		if (entityhits != null && assetunsorted == null)
		{
			inReq.putPageValue("totalhits", entityhits.size());
		}
		else if (entityhits == null && assetunsorted != null)
		{
			inReq.putPageValue("totalhits", assetunsorted.size());
		}
		else
		{
			inReq.putPageValue("totalhits", entityhits.size() + assetunsorted.size());
		}

		getResultsManager().loadOrganizedResults(inReq, entityhits, assetunsorted);

		/**
		 * log.info("Searching as:" + inReq.getUser().getName()); MediaArchive archive = getMediaArchive();
		 * 
		 * Collection<String> keywords = searchArgs.getKeywords();
		 * 
		 * String plainquery = String.join(" ", keywords);
		 * 
		 * QueryBuilder dq =
		 * archive.query("modulesearch").addFacet("entitysourcetype").freeform("description",
		 * plainquery).hitsPerPage(30); dq.getQuery().setIncludeDescription(true);
		 * 
		 * Collection searchmodules = getResultsManager().loadUserSearchTypes(inReq,
		 * searchArgs.getSelectedModuleIds());
		 * 
		 * Collection searchmodulescopy = new ArrayList(searchmodules); searchmodulescopy.remove("asset");
		 * dq.getQuery().setValue("searchtypes", searchmodulescopy);
		 * 
		 * 
		 * HitTracker unsorted = dq.search(inReq);
		 * 
		 * log.info(unsorted);
		 * 
		 * Map<String,String> keywordsLower = new HashMap();
		 * 
		 * getResultsManager().collectMatches(keywordsLower, plainquery, unsorted);
		 * 
		 * inReq.putPageValue("modulehits", unsorted); inReq.putPageValue("livesearchfor", plainquery);
		 * 
		 * List finallist = new ArrayList();
		 * 
		 * for (Iterator iterator = keywordsLower.keySet().iterator(); iterator.hasNext();) { String keyword
		 * = (String) iterator.next(); String keywordcase = keywordsLower.get(keyword);
		 * finallist.add(keywordcase); }
		 * 
		 * Collections.sort(finallist);
		 * 
		 * 
		 * inReq.putPageValue("livesuggestions", finallist); inReq.putPageValue("highlighter", new
		 * Highlighter());
		 * 
		 * int assetmax = 15; if( unsorted.size() > 10) { assetmax = 5; }
		 * 
		 * QueryBuilder assetdq = archive.query("asset") .freeform("description", plainquery)
		 * .hitsPerPage(assetmax);
		 * 
		 * HitTracker assetunsorted = assetdq.search(inReq);
		 * getResultsManager().collectMatches(keywordsLower, plainquery, assetunsorted);
		 * inReq.putPageValue("assethits", assetunsorted);
		 * 
		 * Collection pageOfHits = unsorted.getPageOfHits(); pageOfHits = new ArrayList(pageOfHits);
		 * 
		 * String[] excludeentityids = new String[unsorted.size()]; String[] excludeassetids = new
		 * String[assetunsorted.size()];
		 * 
		 * StringBuilder contextString = new StringBuilder();
		 * 
		 * int idx = 0; for (Object entity : unsorted.getPageOfHits()) { Data d = (Data) entity;
		 * 
		 * String parentassetid = d.get("parentasset"); if(parentassetid != null) { String fulltext =
		 * d.get("longdescription"); if(fulltext == null || fulltext.length() == 0) { Asset parent =
		 * archive.getAsset(parentassetid); fulltext = parent.get("fulltext"); } if(fulltext != null &&
		 * fulltext.length() > 0) { contextString.append("From " + d.getName() + "\n");
		 * contextString.append(fulltext); contextString.append("\n\n"); } } excludeentityids[idx] =
		 * d.getId(); idx++; } idx = 0; for (Object asset : assetunsorted.getPageOfHits()) { Data d = (Data)
		 * asset;
		 * 
		 * String fulltext = d.get("longdescription"); if(fulltext != null && fulltext.length() > 0) {
		 * contextString.append("From " + d.getName() + "\n");
		 * contextString.append(fulltext); contextString.append("\n\n"); }
		 * 
		 * excludeassetids[idx] = d.getId(); idx++; } inReq.putPageValue("excludeentityids",
		 * excludeentityids); inReq.putPageValue("excludeassetids", excludeassetids);
		 * 
		 * inReq.putPageValue("totalhits", unsorted.size() + assetunsorted.size());
		 * 
		 * getResultsManager().loadOrganizedResults(inReq, unsorted,assetunsorted);
		 * 
		 * if( contextString.length() > 0) { Data ragcontext =
		 * archive.getSearcher("ragcontext").createNewData(); ragcontext.setValue("", ""); }
		 */

	}

	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog)
	{
		SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter");

		// Create batch of english words that describe how to search all these things
		Schema schema = loadSchema();

		Searcher embedsearcher = getMediaArchive().getSearcher("aifunctionparameter");
		Collection<SemanticAction> actions = new ArrayList();

		// Special case for asset table
		addAssetTypes(actions);

		for (Iterator iterator = schema.getModules().iterator(); iterator.hasNext();)
		{
			Data parentmodule = (Data) iterator.next();

			Collection existing = embedsearcher.query().exact("aifunction", "search_tables").exact("parentmodule", parentmodule.getId()).search();
			if (!existing.isEmpty())
			{
				log.info("Skipping " + parentmodule);
				continue;
			}

			SemanticAction action = new SemanticAction();

			action.setAiFunction("search_tables");
			action.setSemanticText("Search for " + parentmodule.getName());
			action.setParentData(parentmodule);
			actions.add(action);
			// action = new SemanticAction();
			// action.setParentData(parentmodule);
			// action.setAiFunction("createRecord");
			// action.setSemanticText("Create a new " + parentmodule.getName());
			// actions.add(action);

			// Check for child views
			Collection<Data> children = schema.getChildrenOf(parentmodule.getId());

			for (Iterator iterator2 = children.iterator(); iterator2.hasNext();)
			{
				Data childmodule = (Data) iterator2.next();
				action = new SemanticAction();
				action.setParentData(parentmodule);
				action.setChildData(childmodule);
				action.setAiFunction("search_tables");
				action.setSemanticText("Search for " + childmodule.getName() + " in " + parentmodule.getName());
				actions.add(action);
			}
			populateVectors(manager, actions);
		}
		Collection tosave = new ArrayList();

		for (Iterator iterator = actions.iterator(); iterator.hasNext();)
		{
			SemanticAction semanticAction = (SemanticAction) iterator.next();
			Data data = embedsearcher.createNewData();
			data.setName(semanticAction.getSemanticText());
			data.setValue("aifunction", "search_tables");
			data.setValue("parentmodule", semanticAction.getParentData().getId());
			if (semanticAction.getChildData() != null)
			{
				data.setValue("childmodule", semanticAction.getChildData().getId());
			}
			data.setValue("vectorarray", semanticAction.getVectors());
			tosave.add(data);
		}
		embedsearcher.saveAllData(tosave, null);
		manager.reBalance(inLog); /// Sorts em

		return actions;
		// List<Double> tosearch = manager.makeVector("Find all records in US States in
		// 2023");
		// Collection<RankedResult> results = manager.searchNearestItems(tosearch);
		// log.info(results);

	}

	protected void addAssetTypes(Collection<SemanticAction> actions)
	{

		Collection typesstring = new ArrayList();

		typesstring.add("files");
		typesstring.add("assets");
		typesstring.add("videos");
		typesstring.add("documents");
		typesstring.add("photos");
		typesstring.add("audio");

		Data assetmodule = getMediaArchive().getCachedData("module", "asset");
		typesstring.remove(assetmodule.getName().toLowerCase());
		for (Iterator iterator = typesstring.iterator(); iterator.hasNext();)
		{
			String assettype = (String) iterator.next();
			SemanticAction action = new SemanticAction();
			action.setAiFunction("search_tables");
			action.setSemanticText("Search for " + assettype);
			action.setParentData(assetmodule);
			actions.add(action);
		}

	}

	public Collection<String> makeSearchSuggestions(UserProfile inUserProfile)
	{
		int count = 0;
		Collection<String> suggestions = new ArrayList<String>();
		for (Iterator iterator = inUserProfile.getEntities().iterator(); iterator.hasNext();)
		{
			MultiValued module = (MultiValued) iterator.next();
			if (module.getBoolean("showonsearch") && !module.getId().equals("asset") && count < 5)
			{
				Data entity = getMediaArchive().query(module.getId()).searchOne();
				if (entity != null)
				{
					String entityname = entity.getName();
					String prefix = "";
					if (count % 3 == 0)
					{
						prefix = "Search for ";
					}
					else
					{
						prefix = "Find ";
					}
					prefix = prefix + entityname + " in " + module.getName();
					suggestions.add(prefix);
					count++;
				}
			}
		}
		return suggestions;
	}
}
