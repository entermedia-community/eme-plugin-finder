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
import org.entermediadb.profile.DirectMessageManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.users.User;

public class EmeServerModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(EmeServerModule.class);

	public void getEMEServers(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String searchquery = inReq.getRequestParameter("query");
		String category = inReq.getRequestParameter("category");
		inReq.putPageValue("emeservers", searchServers(archive, inReq.getUser().getId(), searchquery, category));
	}

	/**
	 * EME servers whose name contains the query, optionally in one category (null or "all" for every
	 * category). Each server is a property map with a single "category" id and "joined" set to "true"
	 * for the servers the user has joined.
	 */
	public Collection<Map> searchServers(MediaArchive archive, String inUserId, String searchquery, String category)
	{
		QueryBuilder builder = archive.query("emeserver");

		if (searchquery != null && !searchquery.trim().isEmpty())
		{
			builder.contains("name", searchquery.trim());
		}

		if (category != null && !category.equalsIgnoreCase("all"))
		{
			builder.exact("servercategory", category);
		}

		Collection<Data> emeservers = builder.search();

		Collection<Map> emeserversmap = new ArrayList<>();
		for (Data emeserver : emeservers)
		{
			Map map = emeserver.getProperties();
			ArrayList categories = (ArrayList) map.get("servercategory");
			if (categories != null && !categories.isEmpty())
			{
				map.put("servercategory", categories.get(0));
			}
			emeserversmap.add(map);
		}

		Collection<Data> joinedEMEservers = archive.query("emeserveruser").exact("user", inUserId).search();
		Collection<String> joinedserverids = joinedEMEservers.stream().map(d -> d.get("emeserver")).collect(Collectors.toList());
		for (Map emeserver : emeserversmap)
		{
			if (joinedserverids.contains(emeserver.get("id")))
			{
				emeserver.put("joined", "true");
			}
		}
		return emeserversmap;
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

	/**
	 * chat/connect.json: opens the user's chat with an EME server in its latest session (the channel with
	 * the latest activity, refreshdate), however old. Whether to continue an old session or start a new one
	 * is the user's choice (the app asks, then calls newsession.json). createnew=true starts a new session
	 * right away. The other sessions stay available in the session history (sessions.json).
	 */
	public void handleChatConnection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String serverid = inReq.getRequestParameter("serverid");
		boolean createnew = Boolean.parseBoolean(inReq.getRequestParameter("createnew"));

		Data channel = null;
		if (!createnew)
		{
			channel = findServerChannel(archive, serverid, inReq.getUser().getId());
		}
		if (channel == null)
		{
			channel = createServerChannel(archive, serverid, inReq.getUser());
		}

		inReq.putPageValue("currentchannel", channel);
	}

	protected Data findServerChannel(MediaArchive archive, String serverid, String userid)
	{
		return archive.query("channel").exact("searchtype", "emeserver").exact("dataid", serverid).exact("user", userid).sort("refreshdateDown").searchOne();
	}

	protected Data createServerChannel(MediaArchive archive, String serverid, User user)
	{
		MultiValued server = (MultiValued) archive.getData("emeserver", serverid);
		if (server == null)
		{
			throw new IllegalArgumentException("Unknown EME server: " + serverid);
		}
		Searcher channelsearcher = archive.getSearcher("channel");
		Data channel = channelsearcher.createNewData();
		channel.setName(user.getName() + " - " + server.getName());
		channel.setValue("searchtype", "emeserver");
		channel.setValue("dataid", serverid);
		channel.setValue("channeltype", "agentserverchat");
		channel.setValue("user", user.getId());
		channel.setValue("refreshdate", new Date());
		channelsearcher.saveData(channel, user);
		return channel;
	}

	/**
	 * chat/newsession.json: starts a new session (channel) in the chat that sessions.json lists, picked by
	 * serverid, collectionid or touser, and returns its id. An EME server chat is created from the server;
	 * the others copy the type of the user's latest session there, so they need one to exist already.
	 */
	public void createChatSession(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		User user = inReq.getUser();
		if (user == null)
		{
			inReq.putPageValue("error", "not logged in");
			return;
		}
		String[] chat = findChatTarget(archive, inReq);
		if (chat == null)
		{
			inReq.putPageValue("error", "serverid, collectionid or touser is required");
			return;
		}

		Data channel;
		if ("emeserver".equals(chat[0]))
		{
			channel = createServerChannel(archive, chat[1], user);
		}
		else if (chat[2] != null)
		{
			// Direct message: the collection of the two users, created on first use
			DirectMessageManager dms = (DirectMessageManager) archive.getBean("directMessageManager");
			Data collection = dms.loadDirectMessageCollection(archive, user.getId(), chat[2], true);
			channel = dms.createChannel(archive, collection, user.getId(), chat[2]);
		}
		else
		{
			Data latest = archive.query("channel").exact("searchtype", chat[0]).exact("dataid", chat[1]).exact("user", user.getId()).sort("refreshdateDown").searchOne();
			if (latest == null)
			{
				inReq.putPageValue("error", "no existing chat to start a new session in");
				return;
			}
			Searcher channelsearcher = archive.getSearcher("channel");
			channel = channelsearcher.createNewData();
			channel.setName(latest.getName());
			channel.setValue("searchtype", chat[0]);
			channel.setValue("dataid", chat[1]);
			channel.setValue("channeltype", latest.get("channeltype"));
			channel.setValue("user", user.getId());
			channel.setValue("refreshdate", new Date());
			channelsearcher.saveData(channel, user);
		}
		inReq.putPageValue("currentchannel", channel);
		if ("emeserver".equals(chat[0]))
		{
			// Lets AgentModule.sendWelcomeIfNeeded (newsession.xconf) welcome the new session
			inReq.setRequestParameter("channel", channel.getId());
		}
	}

	/**
	 * The chat a request is about: {searchtype, dataid, touser}. touser is set for a direct message, whose
	 * sessions belong to both users (dataid is then their collection, null before their first message);
	 * null when the request names no chat.
	 */
	protected String[] findChatTarget(MediaArchive archive, WebPageRequest inReq)
	{
		String serverid = inReq.getRequestParameter("serverid");
		String collectionid = inReq.getRequestParameter("collectionid");
		String touser = inReq.getRequestParameter("touser");
		if (Boolean.parseBoolean(inReq.getRequestParameter("myeme")))
		{
			// My EME: the chat with your own emeprofile, found by its owner (the login)
			Data myemeprofile = archive.query("emeprofile").exact("owner", inReq.getUser().getId()).cachedSearchOne();
			return new String[] { "emeprofile", myemeprofile == null ? null : myemeprofile.getId(), null };
		}
		if (serverid != null && !serverid.isEmpty())
		{
			return new String[] { "emeserver", serverid, null };
		}
		if (collectionid != null && !collectionid.isEmpty())
		{
			return new String[] { "librarycollection", collectionid, null };
		}
		if (touser != null && !touser.isEmpty())
		{
			DirectMessageManager dms = (DirectMessageManager) archive.getBean("directMessageManager");
			Data collection = dms.loadDirectMessageCollection(archive, inReq.getUser().getId(), touser, false);
			return new String[] { "librarycollection", collection == null ? null : collection.getId(), touser };
		}
		return null;
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

	/**
	 * Lists the calling user's chat sessions (channels) of one chat, newest activity first. The chat is
	 * picked by serverid (EME server), collectionid (project team chat), touser (direct message) or myeme=true (the user's own emeprofile); the
	 * user always comes from the login, never from the request parameters. Each session is a property
	 * map with the channel id, name, refreshdate and the text of its last non-system message.
	 */
	public void loadChatSessions(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		if (inReq.getUser() == null)
		{
			inReq.putPageValue("error", "not logged in");
			return;
		}

		String[] chat = findChatTarget(archive, inReq);
		if (chat == null)
		{
			inReq.putPageValue("error", "serverid, collectionid, touser or myeme is required");
			return;
		}
		Collection<Data> channels = new ArrayList<Data>();
		if (chat[1] != null)
		{
			QueryBuilder query = archive.query("channel").exact("searchtype", chat[0]).exact("dataid", chat[1]);
			if (chat[2] == null)
			{
				// A direct message's sessions are shared by both users, whoever started them
				query.exact("user", inReq.getUser().getId());
			}
			channels = query.sort("refreshdateDown").hitsPerPage(100).search(inReq);
		}

		Collection<Map> sessions = new ArrayList<Map>();
		for (Data channel : channels)
		{
			Data last = archive.query("chatterbox").exact("channel", channel.getId()).not("messagetype", "system").sort("dateDown").hitsPerPage(1).searchOne();
			Map session = new java.util.HashMap();
			session.put("channel", channel.getId());
			session.put("name", channel.getName());
			Date refresh = ((MultiValued) channel).getDate("refreshdate");
			session.put("refreshdate", refresh == null ? "" : String.valueOf(refresh.getTime()));
			session.put("lastmessage", last == null ? "" : last.get("message"));
			sessions.add(session);
		}
		inReq.putPageValue("sessions", sessions);
	}

	/**
	 * events/emeserver/saved: keeps the emeserver's server user, its ownership and its login key
	 * (serverkey) in step with the record.
	 */
	public void savedEmeServer(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String serverid = inReq.getRequestParameter("dataid");
		if (serverid == null)
		{
			serverid = inReq.getRequestParameter("id");
		}
		Data server = serverid == null ? null : archive.getData("emeserver", serverid);
		if (server == null)
		{
			log.info("emeserver saved event without a known server: " + serverid);
			return;
		}
		getAccountManager(archive).ensureServerAccount(server);
	}

	/**
	 * services/serverkey.json: this server's public key and key id (from .env, bin/serverkey.sh), for
	 * the other side to store (emeserver.serverkey on the root, rootserverkey on a child).
	 */
	public void loadServerPubKey(WebPageRequest inReq)
	{
		EmeServerAccountManager accounts = getAccountManager(getMediaArchive(inReq));
		String publickey = accounts.getOwnEnv(EmeServerAccountManager.ENV_PUBLIC_KEY);
		if (publickey == null)
		{
			inReq.putPageValue("error", EmeServerAccountManager.ENV_PUBLIC_KEY + " missing in .env; run bin/serverkey.sh --show");
			if (inReq.getResponse() != null)
			{
				inReq.getResponse().setStatus(404);
			}
			return;
		}
		inReq.putPageValue("publickey", publickey);
		inReq.putPageValue("keyid", EmeServerAccountManager.keyId(publickey));
	}

	protected EmeServerAccountManager getAccountManager(MediaArchive inArchive)
	{
		return (EmeServerAccountManager) getModuleManager().getBean(inArchive.getCatalogId(), "emeServerAccountManager");
	}

	/**
	 * Notification hub endpoint (services/module/emeserver/notify.json): an EME Server asks this root
	 * server to push to one of its users. Takes serverid, userid, title, message and optional channel,
	 * as form fields or JSON; a JSON body may add a "data" object of extra string values for the app.
	 */
	public void notifyUser(WebPageRequest inReq)
	{
		String serverid = inReq.getRequestParameter("serverid"); // since request is auth guarded the userid should be the server id
		String userid = inReq.getRequestParameter("userid");
		String title = inReq.getRequestParameter("title");
		String message = inReq.getRequestParameter("message");

		Map<String, String> data = new java.util.HashMap<String, String>();
		Map json = inReq.getJsonRequest();
		if (json != null && json.get("data") instanceof Map)
		{
			for (Object entry : ((Map) json.get("data")).entrySet())
			{
				Map.Entry e = (Map.Entry) entry;
				if (e.getKey() != null && e.getValue() != null)
				{
					data.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
				}
			}
		}
		String channel = inReq.getRequestParameter("channel");
		if (channel != null && !channel.isEmpty())
		{
			data.put("channel", channel);
		}

		MediaArchive archive = getMediaArchive(inReq);
		NotificationHubManager hub = (NotificationHubManager) getModuleManager().getBean(archive.getCatalogId(), "notificationHubManager");
		NotificationHubManager.NotifyResult result = hub.notifyUser(inReq.getUser(), serverid, userid, title, message, data);

		inReq.putPageValue("notifyresult", result);
		if (inReq.getResponse() != null)
		{
			inReq.getResponse().setStatus(result.getStatus());
		}
	}

}
