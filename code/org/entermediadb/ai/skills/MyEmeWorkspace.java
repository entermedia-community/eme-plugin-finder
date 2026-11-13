package org.entermediadb.ai.skills;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;

/**
 * The folder a My EME user's OpenCode sessions work in, and the permissions those sessions run with.
 *
 * Every user has a folder in the home of the user the server runs as (in Docker /home/entermedia/workspaces/[userid]).
 * It is the session folder, so the user's files are in it, next to the config OpenCode reads from it:
 *
 *   [userid]/opencode.json     the user's own OpenCode config (from the template opencodeuser.json). Written by the server only.
 *   [userid]/.agents/skills/   the user's own skills
 *
 * The sessions may read these but never change them (see PROTECTED): a session that could write them could add an
 * MCP server, instructions or a skill that every later session of that user would load. Sessions may not leave the
 * folder either. The folder must not sit inside another project: OpenCode also loads skills from every parent
 * folder, and a session in a git repository can read all of it. A user's id can never leave the workspaces
 * folder (see requireSafeUserId).
 */
public class MyEmeWorkspace
{
	/** Letters, digits and . _ @ - only, starting with a letter or digit, so an id can never walk out of the workspaces folder */
	protected static final Pattern SAFE_USER_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._@-]*$");

	/** A skill is a folder with a SKILL.md in it; the folder name is the skill's id */
	protected static final String SKILL_FILE = "SKILL.md";

	/** Environment variable that moves the workspaces folder (default: workspaces in the home of the user running the server) */
	public static final String ROOT_ENV = "EME_WORKSPACES_ROOT";

	public static String requireSafeUserId(String inUserId)
	{
		if (inUserId == null || !SAFE_USER_ID.matcher(inUserId).matches() || inUserId.contains(".."))
		{
			throw new OpenEditException("Not a usable user id for a My EME workspace: " + inUserId);
		}
		return inUserId;
	}

	/** The folder that holds every user's folder */
	public static File root()
	{
		String configured = System.getenv(ROOT_ENV);
		if (configured != null && !configured.trim().isEmpty())
		{
			return new File(configured.trim());
		}
		return new File(System.getProperty("user.home"), "workspaces");
	}

	/** The user's own folder (holds their config and skills). The user id comes from the chat channel, never from a request. */
	public static File userFolder(String inUserId)
	{
		return new File(root(), requireSafeUserId(inUserId));
	}

	/** What a session may not write: every file OpenCode loads config, instructions or skills from in the session folder */
	protected static final String[] PROTECTED = { "opencode.json", "opencode.jsonc", "AGENTS.md", "CLAUDE.md", ".agents/**", ".claude/**", ".opencode/**" };

	/** Written as {userid} in the template; replaced with the user's id in the copy each user gets */
	public static final String USER_ID_PLACEHOLDER = "{userid}";

	/** What a user's own opencode.json starts as when the template is not there */
	protected static final String STARTER_CONFIG = "{\n\t\"$schema\": \"https://opencode.ai/config.json\"\n}\n";

	/**
	 * The user's folder, which is the session folder, created on first use together with the user's own opencode.json, copied from the
	 * template webapp/system/workspaces/opencodeuser.json, with every {userid} in it replaced by the user's id
	 * (an existing one is never touched, so a changed template only reaches new users). This is the folder to give OpenCode as the session's directory.
	 */
	public static File folderFor(MediaArchive inArchive, String inUserId)
	{
		File user = userFolder(inUserId);
		if (!user.isDirectory() && !user.mkdirs() && !user.isDirectory())
		{
			throw new OpenEditException("Could not create the My EME workspace " + user.getAbsolutePath());
		}
		File config = new File(user, "opencode.json");
		if (!config.exists())
		{
			try
			{
				// the id passed requireSafeUserId, so it is safe inside JSON
				String text = userConfigTemplate(inArchive).replace(USER_ID_PLACEHOLDER, inUserId);
				Files.write(config.toPath(), text.getBytes(StandardCharsets.UTF_8));
			}
			catch (IOException e)
			{
				throw new OpenEditException("Could not write " + config.getAbsolutePath(), e);
			}
		}
		return user;
	}

	/** The text of the user config template, or a bare starter when there is no template (or no archive to find it with) */
	protected static String userConfigTemplate(MediaArchive inArchive)
	{
		if (inArchive == null)
		{
			return STARTER_CONFIG;
		}
		ContentItem template = inArchive.getPageManager().getRepository().get("/system/workspaces/opencodeuser.json");
		File file = new File(template.getAbsolutePath());
		if (!file.isFile())
		{
			return STARTER_CONFIG;
		}
		try
		{
			return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new OpenEditException("Could not read " + file.getAbsolutePath(), e);
		}
	}

	/** The ids of the skills in a folder of skills, empty when there is none */
	protected static Set<String> skillIds(File inSkillsFolder)
	{
		Set<String> ids = new LinkedHashSet<String>();
		File[] children = inSkillsFolder == null ? null : inSkillsFolder.listFiles();
		if (children != null)
		{
			for (File child : children)
			{
				if (child.isDirectory() && new File(child, SKILL_FILE).isFile())
				{
					ids.add(child.getName());
				}
			}
		}
		return ids;
	}

	/** The shared skills every user gets (webapp/system/workspaces/skills) and the user's own, by id */
	public static Set<String> skillsFor(MediaArchive inArchive, String inUserId)
	{
		Set<String> ids = new LinkedHashSet<String>();
		ContentItem shared = inArchive.getPageManager().getRepository().get("/system/workspaces/skills");
		ids.addAll(skillIds(new File(shared.getAbsolutePath())));
		ids.addAll(skillIds(new File(userFolder(inUserId), ".agents/skills")));
		return ids;
	}

	/**
	 * Rules for the user's sessions, in OpenCode's {action, resource, effect} form. Later rules override earlier
	 * ones, so everything is allowed first and the exceptions follow. The shell and anything outside the session
	 * folder are denied, as is writing the config and skills in it (PROTECTED), as are the web tools and sub-agents. Only the shared skills and the user's own may be
	 * used (the personal skills of whoever runs the OpenCode service, and the built-in ones, are hidden). A denied
	 * tool fails in the session (a session.tool.failed event) and nobody is asked for approval.
	 */
	@SuppressWarnings("unchecked")
	public static JSONArray permissions(MediaArchive inArchive, String inUserId)
	{
		JSONArray rules = new JSONArray();
		rules.add(rule("*", "*", "allow"));
		// "question" stays denied until the chat can show OpenCode's question forms and send the answers back
		for (String denied : new String[] { "shell", "external_directory", "webfetch", "websearch", "browser", "question", "subagent" })
		{
			rules.add(rule(denied, "*", "deny"));
		}
		for (String path : PROTECTED)
		{
			rules.add(rule("edit", path, "deny"));
		}
		rules.add(rule("skill", "*", "deny"));
		for (String skill : skillsFor(inArchive, inUserId))
		{
			rules.add(rule("skill", skill, "allow"));
		}
		return rules;
	}

	@SuppressWarnings("unchecked")
	protected static JSONObject rule(String inAction, String inResource, String inEffect)
	{
		JSONObject rule = new JSONObject();
		rule.put("action", inAction);
		rule.put("resource", inResource);
		rule.put("effect", inEffect);
		return rule;
	}
}
