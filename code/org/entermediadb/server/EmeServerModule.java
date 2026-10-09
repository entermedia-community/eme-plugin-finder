package org.entermediadb.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;

public class EmeServerModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(EmeServerModule.class);

	public void getEMEServers(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String searchquery = inReq.getRequestParameter("query");
		String category = inReq.getRequestParameter("category");

		Searcher serversearcher = archive.getSearcher("emeserver");

		QueryBuilder builder = serversearcher.query();

		if (searchquery != null)
		{
			builder.contains("name", searchquery);
		}

		if (category != null && !category.equalsIgnoreCase("all"))
		{
			builder.exact("category", category);
		}

		Collection<Data> emeservers = builder.search();

		Collection<Map> emeserversmap = new ArrayList<>();
		for (Data emeserver : emeservers)
		{
			Map map = emeserver.getProperties();
			ArrayList categories = (ArrayList) map.get("category");
			if (categories != null && !categories.isEmpty())
			{
				map.put("category", categories.get(0));
			}
			emeserversmap.add(map);
		}

		Collection<Data> joinedEMEservers = archive.query("emeserveruser").exact("user", inReq.getUser().getId()).search();
		Collection<String> joinedserverids = joinedEMEservers.stream().map(d -> d.get("emeserver")).collect(Collectors.toList());
		for (Map emeserver : emeserversmap)
		{
			if (joinedserverids.contains(emeserver.get("id")))
			{
				emeserver.put("joined", "true");
			}
		}
		inReq.putPageValue("emeservers", emeserversmap);
	}

	public void getEMEServer(WebPageRequest inReq)
	{
		String serverid = inReq.getRequestParameter("serverid");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("emeserver");
		Data server = searcher.loadData(serverid);

		Data joined = archive.query("emeserveruser").exact("user", inReq.getUser().getId()).exact("emeserver", serverid).searchOne();
		if (joined != null)
		{
			inReq.putPageValue("joined", true);
		}
		else
		{
			inReq.putPageValue("joined", false);
		}
		inReq.putPageValue("emeserver", server);
	}

	public void joinEMEServer(WebPageRequest inReq)
	{
		String serverid = inReq.getRequestParameter("serverid");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher usersearcher = archive.getSearcher("emeserveruser");
		String userid = inReq.getUser().getId();
		Data emeserveruser = usersearcher.query().exact("user", userid).exact("emeserver", serverid).searchOne();
		if (emeserveruser == null)
		{
			emeserveruser = usersearcher.createNewData();
			emeserveruser.setValue("joined", new Date());
			emeserveruser.setValue("user", userid);
			emeserveruser.setValue("emeserver", serverid);
			usersearcher.saveData(emeserveruser);

			Searcher serversearcher = archive.getSearcher("emeserver");
			MultiValued server = (MultiValued) archive.getData("emeserver", serverid);
			if (server == null)
			{
				throw new RuntimeException("Server not found: " + serverid);
			}
			Integer membercount = server.getInt("membercount");
			if (membercount == null)
			{
				membercount = 0;
			}
			server.setValue("membercount", membercount + 1);
			serversearcher.saveData(server, inReq.getUser());
		}
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
			MultiValued server = (MultiValued) archive.getData("emeserver", serverid);
			if (server != null)
			{
				Integer membercount = server.getInt("membercount");
				if (membercount == null)
				{
					membercount = 0;
				}
				server.setValue("membercount", membercount - 1);
				searcher.saveData(server);
			}

		}
	}

	public void getUserEMEServers(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Collection<Data> userservers = archive.query("emeserveruser").exact("user", inReq.getUser().getId()).search();
		Collection<String> serverids = userservers.stream().map(d -> d.get("emeserver")).collect(Collectors.toList());
		if (serverids.isEmpty())
		{
			inReq.putPageValue("emeservers", new ArrayList<>());
			return;
		}
		Collection<Data> emeservers = archive.query("emeserver").ids(serverids).search();
		inReq.putPageValue("emeservers", emeservers);
	}

	public void getServerCategories(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("servercategory");
		Collection<Data> categories = searcher.query().all().search();
		inReq.putPageValue("categories", categories);
	}

	public void handleChatConnection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String serverid = inReq.getRequestParameter("serverid");

		Searcher channelsearcher = archive.getSearcher("channel");
		Data channel = channelsearcher.query().exact("searchtype", "emeserver").exact("dataid", serverid).searchOne();
		if (channel == null)
		{
			MultiValued server = (MultiValued) archive.getData("emeserver", serverid);
			channel = channelsearcher.createNewData();
			channel.setName(inReq.getUser().getName() + " - " + server.getName());
			channel.setValue("searchtype", "emeserver");
			channel.setValue("dataid", serverid);
			channel.setValue("channeltype", "emeteamchat");
			channel.setValue("user", inReq.getUser().getId());
			channelsearcher.saveData(channel, inReq.getUser());
		}

		inReq.putPageValue("channel", channel);
	}

	public void loadChat(WebPageRequest inReq)
	{
		String channelId = inReq.getRequestParameter("channel");
		if (channelId == null)
		{
			log.warn("Missing channel in request for loadChat");
			return;
		}

		Data channel = getMediaArchive(inReq).query("channel").exact("id", channelId).searchOne();
		if (channel == null)
		{
			log.warn("Channel not found for id: " + channelId);
			return;
		}

		Collection<Data> messages = getMediaArchive(inReq).query("chatterbox").exact("channel", channelId).sort("dateDown").search(inReq);

		inReq.putPageValue("messages", messages);
	}

}
