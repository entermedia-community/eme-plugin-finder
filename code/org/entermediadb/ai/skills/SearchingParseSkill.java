package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.ai.assistant.AiSearchTable;
import org.entermediadb.ai.classify.SemanticTableManager;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

public class SearchingParseSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(SearchingParseSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		MultiValued inAgentMessage = messageContext.getAgentMessage();

		// parse the search first
		// If no parts then keep talking
		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));

		String query = null;
		if (usermessage != null)
		{
			query = usermessage.get("message");
		}
		else 
		{
			query = (String) messageContext.getContextValue("userquery");
		}

		inAgentContext.put("userquery", query);

		inAgentContext.setMessagePrefix(null);
		inAgentContext.setAiSearchParams(null);

		LlmConnection server = getMediaArchive().getLlmConnection("thinking");
		LlmResponse res = server.callStructure(inAgentContext, "search_parse");
		JSONObject messagestructured = (JSONObject) res.getResponsePayload();

		loadSearchParts(inAgentContext, messagestructured);
		
		if (inAgentContext.getAiSearchParams() == null)
		{
			Map details = (Map) messagestructured.get("step_details");
			String friendly_response = (String) details.get("friendly_response");
			if (friendly_response == null)
			{
				friendly_response = (String)((Map)details.get("conversation")).get("friendly_response");
			}
			res.setRawMessage(friendly_response);
			messageContext.setLastResponse(res);
			messageContext.fireStatusComplete(messageContext.getCurrentAgentJobStep());
			return;
		}

		messageContext.setLastResponse(res);
		super.process(inAgentContext);

	}

	protected void loadSearchParts(AgentContext inAgentContext, JSONObject structure, String messageText)
	{
		ArrayList tables = (ArrayList) structure.get("tables");
		String type = null;

		if (tables == null)
		{
			return;
		}

		log.info("AI Assistant Searching Tables: " + structure.toJSONString());

		AiSearch search = new AiSearch();
		search.setOriginalSearchString(messageText);

		search.setStep1(null);
		search.setStep2(null);
		search.setStep3(null);

		for (Iterator iterator = tables.iterator(); iterator.hasNext();)
		{
			JSONObject jsontable = (JSONObject) iterator.next();
			AiSearchTable searchtable = new AiSearchTable();

			Map parameters = (Map) jsontable.get("parameters");
			searchtable.setParameters(parameters);

			String targetTable = (String) jsontable.get("table_name");

			if ("all".equalsIgnoreCase(targetTable))
			{
				if (parameters.keySet().contains("module"))
				{
					targetTable = (String) parameters.get("module");
				}
				else
				{
					targetTable = null;
				}
			}

			if (targetTable != null)
			{
				String[] check = targetTable.split("\\|");
				if (check.length == 2)
				{
					targetTable = check[1];
				}
				else
				{
					targetTable = check[0];
				}
			}

			searchtable.setTargetTable(targetTable);

			String foreigntablename = (String) jsontable.get("foreign_table");
			if (foreigntablename != null)
			{
				AiSearchTable ftable = new AiSearchTable();
				ftable.setTargetTable(foreigntablename);

				Map foreignparameters = (Map) jsontable.get("foreign_table_parameters");
				ftable.setParameters(foreignparameters);

				searchtable.setForeignTable(ftable);

			}

			if (search.getStep1() == null)
			{
				search.setStep1(searchtable);
			}
			else
			{
				// if( "join".equals( step.get("operation")) ||
				// search.getStep1().getTargetTable().equals(targetTable) )
				// {
				// continue; //Duplicate
				// }
				if (search.getStep2() == null)
				{
					search.setStep2(searchtable);
				}
				else if (search.getStep3() == null)
				{
					search.setStep3(searchtable);
				}
			}

			// if("all".equals(targetTable))
			// {
			// return "search_multiple";
			// }

		}

		if (search.getStep1() != null && search.getStep1().getTargetTable() == null)
		{
			Data modulesearch = getMediaArchive().getCachedData("module", "modulesearch");
			search.getStep1().setModule(modulesearch);
		}
		else
		{

			if (search.getStep2() == null && search.getStep1() != null && search.getStep1().getForeignTable() != null)
			{
				search.setStep2(search.getStep1());

				search.setStep1(search.getStep2().getForeignTable());
			}

			String text = "Search";

			if (search.getStep2() != null)
			{
				text = text + " for " + search.getStep2().getTargetTable() + " in " + search.getStep1().getTargetTable();
			}
			else if (search.getStep1() != null)
			{
				text = text + " for " + search.getStep1().getTargetTable();
			}

			SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter");
			List<Double> tosearch = manager.makeVector(text);
			Collection<RankedResult> suggestions = manager.searchNearestItems(tosearch);
			// Load more details into this request and possibly change the type
			if (!suggestions.isEmpty())
			{
				inAgentContext.setRankedSuggestions(suggestions);
				RankedResult top = (RankedResult) suggestions.iterator().next();
				if (top.getDistance() < .7)
				{
					type = top.getEmbedding().get("aifunction"); // More specific type of search

					processAISearchArgs(structure, top.getEmbedding(), inAgentContext, search);

				}
			}

		}
		inAgentContext.setAiSearchParams(search);
	}

	public void processAISearchArgs(JSONObject airesults, Data inEmbeddingMatch, AgentContext inContext, AiSearch inAiSearch)
	{
		// Search for tomatoes in sales departments
		// airesults

		if (inEmbeddingMatch != null)
		{
			String parentid = inEmbeddingMatch.get("parentmodule");
			Data parentmodule = getMediaArchive().getCachedData("module", parentid);
			if (parentmodule != null)
			{
				inAiSearch.getStep1().setModule(parentmodule);
			}
			if (inAiSearch.getStep2() != null) // Not needed?
			{
				String childid = inEmbeddingMatch.get("childmodule");
				Data childmodule = getMediaArchive().getCachedData("module", childid);
				if (childmodule != null)
				{
					inAiSearch.getStep2().setModule(childmodule);
				}
			}
		}
	}

	protected void loadSearchParts(AgentContext inAgentContext, JSONObject content)
	{
		// TODO: Use IF statements to sort what parsing we need to do. parseSearchParams
		// parseWorkflowParams etc

		JSONObject details = (JSONObject) content.get("step_details");

		if (details == null)
		{
			throw new OpenEditException("No details specified in results: " + content.toJSONString());
		}
		JSONObject parseSearchParts = (JSONObject) details.get("parseSearchParts");
		if (parseSearchParts == null)
		{
			log.info("No structure found for parseSearchParts: " + parseSearchParts);
			return;
		}

		loadSearchParts(inAgentContext, parseSearchParts, (String) inAgentContext.getContextValue("userquery"));

	}
}
