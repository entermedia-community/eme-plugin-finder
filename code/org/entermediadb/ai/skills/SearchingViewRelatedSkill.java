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
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.hittracker.HitTracker;

public class SearchingViewRelatedSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(SearchingViewRelatedSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, agentFn);
		messageContext.setLastResponse(response);
		messageContext.fireStatusComplete(messageContext.getCurrentAgentJobStep());

		return;
	}

	public Collection<Map> getRelatedRecords(String inEntityModuleId, String inEntityId)
	{
		Collection detailsviews = getMediaArchive().query("view").exact("moduleid", inEntityModuleId).exact("systemdefined", false).cachedSearch();

		Collection<Map> relatedrecords = new ArrayList();

		for (Iterator iterator = detailsviews.iterator(); iterator.hasNext();)
		{
			Data view = (Data) iterator.next();

			String listid = view.get("rendertable");
			if (listid != null)
			{

				HitTracker found = getMediaArchive().query(listid).exact(inEntityModuleId, inEntityId).search();
				if (found.size() > 0)
				{
					Map record = new HashMap();
					record.put("name", view.getName());
					record.put("moduleid", listid);
					record.put("total", found.size());

					relatedrecords.add(record);
				}
			}
		}

		return relatedrecords;
	}

	public Collection<Data> getRelatedRecordList(String inEntityModuleId, String inEntityId, String inListId)
	{
		Collection<Data> recordlist = getMediaArchive().query(inListId).exact(inEntityModuleId, inEntityId).search();
		// TODO pagination
		return recordlist;
	}

	public Data getRecord(String inEntityModuleId, String inEntityId, String inListId, String inRecordId)
	{
		Data record = getMediaArchive().query(inListId).exact(inEntityModuleId, inEntityId).exact("id", inRecordId).searchOne();
		return record;
	}
}
