package org.entermediadb.asset.modules;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;

public class EmeServerModule extends BaseMediaModule
{
	public void getEMEServers(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String searchquery = inReq.getRequestParameter("query");
		String category = inReq.getRequestParameter("category");

		Searcher searcher = archive.getSearcher("emeserver");

		QueryBuilder builder = searcher.query();

		if (searchquery != null)
		{
			builder.contains("name", searchquery);
		}

		if (category != null)
		{
			builder.exact("servercategory", category);
		}

		Collection<Data> emeservers = builder.search();
		Collection<Data> joinedEMEservers = searcher.query().exact("user", inReq.getUser().getId()).search();
		Collection<String> joinedserverids = joinedEMEservers.stream().map(Data::getId).collect(Collectors.toList());
		for (Data emeserver : emeservers)
		{
			if (joinedserverids.contains(emeserver.getId()))
			{
				emeserver.setProperty("joined", "true");
			}
		}
		inReq.putPageValue("emeservers", emeservers);
	}

	public void getEMEServer(WebPageRequest inReq)
	{
		String serverid = inReq.getRequestParameter("serverid");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("emeserver");
		Data server = searcher.loadData(serverid);
		inReq.putPageValue("emeserver", server);
	}

	public void joinEMEServer(WebPageRequest inReq)
	{
		String serverid = inReq.getRequestParameter("serverid");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("emeserveruser");
		Data emeserveruser = searcher.createNewData();
		emeserveruser.setValue("user", inReq.getUser().getId());
		emeserveruser.setValue("emeserver", serverid);
		emeserveruser.setValue("joined", new Date());
		searcher.saveData(emeserveruser);
	}

	public void leaveEMEServer(WebPageRequest inReq)
	{
		String serverid = inReq.getRequestParameter("serverid");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("emeserveruser");
		Data emeserveruser = searcher.query().exact("user", inReq.getUser().getId()).exact("emeserver", serverid).searchOne();
		if (emeserveruser != null)
		{
			searcher.delete(emeserveruser, inReq.getUser());
		}
	}

	public void getUserEMEServers(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("emeserveruser");
		Collection<Data> emeservers = searcher.query().exact("user", inReq.getUser().getId()).search();
		inReq.putPageValue("emeservers", emeservers);
	}

}
