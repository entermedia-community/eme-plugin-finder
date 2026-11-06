package org.entermediadb.server;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.google.GoogleManager;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.data.Searcher;
import org.openedit.users.User;

/**
 * Notification hub on the root server (eme.world). The app only receives pushes from the root's Firebase
 * project, so other EME Servers (e.g. impactbank.world) ask the hub to push to a user for them. The hub only
 * pushes when the user is connected to that server (an emeserveruser record), then sends to the user's FCM
 * topic like chat pushes do.
 *
 * Servers call it signed in as their server user (EmeServerAccountManager), which must own the emeserver it
 * posts for.
 */
public class NotificationHubManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(NotificationHubManager.class);

	/** Notification arrived from a child server (recorded on the root) */
	public static final String DIRECTION_RECEIVED = "received";
	/** Notification this child server posted to the root (recorded on the child) */
	public static final String DIRECTION_SENT = "sent";

	public static final int MAX_TITLE = 200;
	public static final int MAX_MESSAGE = 2000;
	public static final int MAX_DATA_ENTRIES = 20;

	/** Data keys the hub sets itself; a server cannot override them. */
	protected static final String[] RESERVED_KEYS = { "type", "serverid", "servername", "serverurl", "title", "body", "notificationtype", "topic" };

	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
	}

	protected GoogleManager getGoogleManager()
	{
		return (GoogleManager) getModuleManager().getBean(getCatalogId(), "googleManager");
	}

	/**
	 * Outcome of a notify request. {@link #getStatus()} is the HTTP status the endpoint answers with.
	 */
	public static class NotifyResult
	{
		protected final int fieldStatus;
		protected final String fieldError;
		protected final String fieldErrorDescription;

		protected NotifyResult(int inStatus, String inError, String inErrorDescription)
		{
			fieldStatus = inStatus;
			fieldError = inError;
			fieldErrorDescription = inErrorDescription;
		}

		public static NotifyResult queued()
		{
			return new NotifyResult(202, null, null);
		}

		public static NotifyResult error(int inStatus, String inError, String inDescription)
		{
			return new NotifyResult(inStatus, inError, inDescription);
		}

		public boolean isSuccess()
		{
			return fieldError == null;
		}

		public int getStatus()
		{
			return fieldStatus;
		}

		public String getError()
		{
			return fieldError;
		}

		public String getErrorDescription()
		{
			return fieldErrorDescription;
		}
	}

	/**
	 * Pushes a notification to inUserId on behalf of EME Server inServerId.
	 *
	 * @param inData optional extra string values for the app (e.g. channel); reserved keys are ignored
	 */
	public NotifyResult notifyUser(User inSender, String inServerId, String inUserId, String inTitle, String inMessage, Map<String, String> inData)
	{
		NotifyResult result = deliver(inSender, inServerId, inUserId, inTitle, inMessage, inData);
		// Only requests naming a known server are tracked; anything else has no emeserver to file it under
		if (!isBlank(inServerId) && getMediaArchive().getData("emeserver", inServerId) != null)
		{
			track(DIRECTION_RECEIVED, inServerId, inUserId, inTitle, inMessage, inData, result);
		}
		return result;
	}

	protected NotifyResult deliver(User inSender, String inServerId, String inUserId, String inTitle, String inMessage, Map<String, String> inData)
	{
		if (inSender == null)
		{
			return NotifyResult.error(401, "unauthorized", "Sign in as the server's user first");
		}
		if (isBlank(inServerId) || isBlank(inUserId) || isBlank(inTitle) || isBlank(inMessage))
		{
			return NotifyResult.error(400, "invalid_request", "serverid, userid, title and message are required");
		}
		if (inTitle.length() > MAX_TITLE || inMessage.length() > MAX_MESSAGE)
		{
			return NotifyResult.error(400, "invalid_request", "title is limited to " + MAX_TITLE + " and message to " + MAX_MESSAGE + " characters");
		}
		if (inData != null && inData.size() > MAX_DATA_ENTRIES)
		{
			return NotifyResult.error(400, "invalid_request", "data is limited to " + MAX_DATA_ENTRIES + " entries");
		}

		MediaArchive archive = getMediaArchive();
		Data server = archive.getData("emeserver", inServerId);
		if (server == null)
		{
			return NotifyResult.error(404, "unknown_server", "No EME Server " + inServerId);
		}
		if (!inSender.getId().equals(server.get("owner")))
		{
			log.info(inSender.getId() + " tried to notify for server " + inServerId + " without owning it");
			return NotifyResult.error(403, "forbidden", "Only the owner of server " + inServerId + " can notify its users");
		}
		User user = archive.getUser(inUserId);
		if (user == null || !user.isEnabled())
		{
			return NotifyResult.error(404, "unknown_user", "No enabled user " + inUserId);
		}

		Searcher links = archive.getSearcher("emeserveruser");
		Data link = links.query().exact("user", inUserId).exact("emeserver", inServerId).searchOne();
		if (link == null)
		{
			// Same answer whether or not the pair ever existed: servers only reach their own members
			return NotifyResult.error(403, "not_connected", "User " + inUserId + " is not connected to server " + inServerId);
		}

		Map<String, String> extra = new HashMap<String, String>();
		if (inData != null)
		{
			for (Iterator<Map.Entry<String, String>> iterator = inData.entrySet().iterator(); iterator.hasNext();)
			{
				Map.Entry<String, String> entry = iterator.next();
				if (entry.getKey() != null && entry.getValue() != null && !isReserved(entry.getKey()))
				{
					extra.put(entry.getKey(), entry.getValue());
				}
			}
		}
		// Lets the app open the right server's chat when the push is tapped
		extra.put("type", "servernotification");
		extra.put("serverid", inServerId);
		putIfSet(extra, "servername", server.getName());
		putIfSet(extra, "serverurl", server.get("servermediadburl"));

		// Sent asynchronously; Firebase errors are logged by GoogleManager
		getGoogleManager().notifyTopic(GoogleManager.userTopic(inUserId), null, inTitle, inMessage, extra);

		link.setValue("lastnotification", inTitle);
		link.setValue("lastnotificationtime", new Date());
		links.saveData(link);

		log.info("Queued push to " + inUserId + " for server " + inServerId);
		return NotifyResult.queued();
	}

	/**
	 * Child server side: asks the root's hub to push to inUserId for this server, signed in on the root as this
	 * server's user. Uses the catalog settings remoteemerauthserver (root mediadb URL, e.g.
	 * https://eme.world/site/mediadb) and emeserverid (this server's emeserver id on the root).
	 */
	public NotifyResult postToRoot(String inUserId, String inTitle, String inMessage, Map<String, String> inData)
	{
		NotifyResult result = sendToRoot(inUserId, inTitle, inMessage, inData);
		String serverid = getMediaArchive().getCatalogSettingValue("emeserverid");
		track(DIRECTION_SENT, serverid, inUserId, inTitle, inMessage, inData, result);
		return result;
	}

	protected NotifyResult sendToRoot(String inUserId, String inTitle, String inMessage, Map<String, String> inData)
	{
		MediaArchive archive = getMediaArchive();
		String root = archive.getCatalogSettingValue("remoteemerauthserver");
		String serverid = archive.getCatalogSettingValue("emeserverid");
		if (isBlank(root) || isBlank(serverid))
		{
			return NotifyResult.error(500, "not_configured", "Catalog settings remoteemerauthserver and emeserverid are required");
		}
		org.json.simple.JSONObject body = new org.json.simple.JSONObject();
		body.put("serverid", serverid);
		body.put("userid", inUserId);
		body.put("title", inTitle);
		body.put("message", inMessage);
		if (inData != null && !inData.isEmpty())
		{
			body.put("data", new org.json.simple.JSONObject(inData));
		}
		try
		{
			EmeServerAccountManager accounts = (EmeServerAccountManager) getModuleManager().getBean(getCatalogId(), "emeServerAccountManager");
			Object[] result = accounts.postJson(root, "/services/module/emeserver/notify.json", EmeServerAccountManager.serverUserId(serverid), body);
			int status = (Integer) result[0];
			if (status == 202 || status == 200)
			{
				return NotifyResult.queued();
			}
			log.info("Hub refused push to " + inUserId + " [" + status + "]: " + result[1]);
			return NotifyResult.error(status, "hub_refused", String.valueOf(result[1]));
		}
		catch (Exception e)
		{
			log.error("Could not post notification to " + root, e);
			return NotifyResult.error(502, "hub_unreachable", e.getMessage());
		}
	}

	/**
	 * Records one notification attempt, delivered or refused, in the emeservernotification table. A failure to
	 * record is logged and never changes the outcome of the push.
	 */
	protected void track(String inDirection, String inServerId, String inUserId, String inTitle, String inMessage, Map<String, String> inData, NotifyResult inResult)
	{
		try
		{
			Searcher searcher = getMediaArchive().getSearcher("emeservernotification");
			Data row = searcher.createNewData();
			putIfSet(row, "emeserver", inServerId);
			putIfSet(row, "user", inUserId);
			row.setValue("direction", inDirection);
			row.setValue("title", truncate(inTitle, MAX_TITLE));
			row.setValue("message", truncate(inMessage, MAX_MESSAGE));
			if (inData != null)
			{
				putIfSet(row, "channel", inData.get("channel"));
			}
			row.setValue("notificationstatus", inResult.isSuccess() ? "queued" : "failed");
			row.setValue("httpstatus", inResult.getStatus());
			if (!inResult.isSuccess())
			{
				row.setValue("error", inResult.getError() + ": " + inResult.getErrorDescription());
			}
			row.setValue("notificationtime", new Date());
			searcher.saveData(row);
		}
		catch (Exception e)
		{
			log.error("Could not record notification for " + inUserId + " / server " + inServerId, e);
		}
	}

	protected void putIfSet(Data inRow, String inKey, String inValue)
	{
		if (!isBlank(inValue))
		{
			inRow.setValue(inKey, inValue);
		}
	}

	protected String truncate(String inValue, int inMax)
	{
		if (inValue == null || inValue.length() <= inMax)
		{
			return inValue;
		}
		return inValue.substring(0, inMax);
	}

	protected boolean isReserved(String inKey)
	{
		for (String reserved : RESERVED_KEYS)
		{
			if (reserved.equals(inKey))
			{
				return true;
			}
		}
		return false;
	}

	protected void putIfSet(Map<String, String> inMap, String inKey, String inValue)
	{
		if (!isBlank(inValue))
		{
			inMap.put(inKey, inValue);
		}
	}

	protected boolean isBlank(String inValue)
	{
		return inValue == null || inValue.trim().isEmpty();
	}
}
