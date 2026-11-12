package org.entermediadb.ai.skills;

import java.io.File;
import java.util.regex.Pattern;

import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;

/**
 * The folder a My EME user's OpenCode sessions work in, and the permissions those sessions run with.
 *
 * Each user has one plain folder (not a git repo, so OpenCode gives it its own project) at
 * /WEB-INF/data/[catalogid]/workspaces/[userid]/. Every chat session of that user is an OpenCode
 * session in that folder.
 */
public class MyEmeWorkspace
{
	/** Letters, digits and . _ @ - only, starting with a letter or digit, so an id can never walk out of workspaces/ */
	protected static final Pattern SAFE_USER_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._@-]*$");

	public static String requireSafeUserId(String inUserId)
	{
		if (inUserId == null || !SAFE_USER_ID.matcher(inUserId).matches() || inUserId.contains(".."))
		{
			throw new OpenEditException("Not a usable user id for a My EME workspace: " + inUserId);
		}
		return inUserId;
	}

	/** The user's workspace folder, created on first use. The user id comes from the chat channel, never from a request. */
	public static File folderFor(MediaArchive inArchive, String inUserId)
	{
		String userid = requireSafeUserId(inUserId);
		ContentItem item = inArchive.getPageManager().getRepository().get("/WEB-INF/data/" + inArchive.getCatalogId() + "/workspaces/" + userid);
		File folder = new File(item.getAbsolutePath());
		if (!folder.isDirectory() && !folder.mkdirs() && !folder.isDirectory())
		{
			throw new OpenEditException("Could not create the My EME workspace " + folder.getAbsolutePath());
		}
		return folder;
	}

	/**
	 * Rules for the session, in OpenCode's {action, resource, effect} form. Later rules override earlier
	 * ones, so everything is allowed first and the exceptions follow. The shell and anything outside the
	 * workspace folder are denied, as are the web tools. A denied tool fails in the session (a
	 * session.tool.failed event) and nobody is asked for approval.
	 */
	@SuppressWarnings("unchecked")
	public static JSONArray permissions()
	{
		JSONArray rules = new JSONArray();
		rules.add(rule("*", "allow"));
		// "question" stays denied until the chat can show OpenCode's question forms and send the answers back
		for (String denied : new String[] { "shell", "external_directory", "webfetch", "websearch", "browser", "question" })
		{
			rules.add(rule(denied, "deny"));
		}
		return rules;
	}

	@SuppressWarnings("unchecked")
	protected static JSONObject rule(String inAction, String inEffect)
	{
		JSONObject rule = new JSONObject();
		rule.put("action", inAction);
		rule.put("resource", "*");
		rule.put("effect", inEffect);
		return rule;
	}
}
