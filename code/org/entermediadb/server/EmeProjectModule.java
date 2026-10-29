package org.entermediadb.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

/**
 * Search, join and leave projects (librarycollection) for the signed-in user.
 * Being on a project's team is a librarycollectionusers row with
 * ontheteam=true, the same rows ProjectManager.isOnTeam and viewUserProjects
 * read. Failures set the "error" page value, which the .json pages report.
 */
public class EmeProjectModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(EmeProjectModule.class);

	/** Projects that cannot be found or joined. */
	private static final String[] CLOSED_STATUSES = { "disabled", "closed", "pendingdelete" };

	/**
	 * Searches open community projects by name (query), and flags the ones
	 * the user is already on the team of. Page values: hits, joinedids.
	 */
	public void searchProjects(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		inReq.putPageValue("hits", searchOpenProjects(archive, inReq, inReq.getRequestParameter("query")));
		inReq.putPageValue("joinedids", getJoinedProjectIds(archive, inReq.getUserName()));
	}

	/** Open community projects whose name contains the query (all when empty), by name. */
	public HitTracker searchOpenProjects(MediaArchive archive, WebPageRequest inReq, String searchquery)
	{
		QueryBuilder builder = archive.query("librarycollection").named("hits");
		builder.orgroup("collectiontype", "1");
		builder.orgroup("priviacylevel", "community");
		for (String status : CLOSED_STATUSES)
		{
			builder.not("organizationstatus", status);
		}
		if (searchquery != null && !searchquery.trim().isEmpty())
		{
			builder.contains("name", searchquery.trim());
		}
		HitTracker hits = builder.sort("name").search(inReq);
		hits.setHitsPerPage(100);
		return hits;
	}

	/** The projects the user is on the team of. Page values: hits, joinedids. */
	public void getUserProjects(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Set<String> ids = getJoinedProjectIds(archive, inReq.getUserName());

		Collection<String> lookup = new ArrayList<String>(ids);
		if (lookup.isEmpty())
		{
			lookup.add("NONE");
		}
		HitTracker hits = archive.query("librarycollection").named("hits").ids(lookup).sort("name").search(inReq);
		hits.setHitsPerPage(100);

		inReq.putPageValue("hits", hits);
		inReq.putPageValue("joinedids", ids);
	}

	/** Puts the user on the team of the project (collectionid). Idempotent. */
	public void joinProject(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Data project = loadProject(inReq, archive);
		if (project == null)
		{
			return;
		}
		String privacy = project.get("priviacylevel");
		if (privacy != null && !privacy.equals("community"))
		{
			inReq.putPageValue("error", "This project is not open to join");
			return;
		}
		for (String status : CLOSED_STATUSES)
		{
			if (status.equals(project.get("organizationstatus")))
			{
				inReq.putPageValue("error", "This project is not open to join");
				return;
			}
		}

		Searcher usersearcher = archive.getSearcher("librarycollectionusers");
		String userid = inReq.getUserName();
		Data membership = usersearcher.query().exact("collectionid", project.getId()).exact("followeruser", userid).searchOne();
		if (membership == null)
		{
			membership = usersearcher.createNewData();
			membership.setValue("collectionid", project.getId());
			membership.setValue("followeruser", userid);
			membership.setValue("addeddate", new Date());
		}
		if (!"true".equals(String.valueOf(membership.getValue("ontheteam"))))
		{
			membership.setValue("ontheteam", true);
			usersearcher.saveData(membership, inReq.getUser());
		}
		inReq.putPageValue("project", project);
	}

	/**
	 * Takes the user off the team of the project (collectionid). Idempotent,
	 * but the last team member cannot leave.
	 */
	public void leaveProject(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Data project = loadProject(inReq, archive);
		if (project == null)
		{
			return;
		}

		Searcher usersearcher = archive.getSearcher("librarycollectionusers");
		Data membership = usersearcher.query().exact("collectionid", project.getId()).exact("followeruser", inReq.getUserName()).exact("ontheteam", "true").searchOne();
		if (membership == null)
		{
			inReq.putPageValue("project", project);
			return;
		}
		int teamsize = usersearcher.query().exact("collectionid", project.getId()).exact("ontheteam", "true").search().size();
		if (teamsize <= 1)
		{
			inReq.putPageValue("error", "You are the only team member, so you cannot leave this project");
			return;
		}
		usersearcher.delete(membership, inReq.getUser());
		inReq.putPageValue("project", project);
	}

	private Data loadProject(WebPageRequest inReq, MediaArchive archive)
	{
		String collectionid = inReq.getRequestParameter("collectionid");
		Data project = collectionid == null ? null : archive.getCachedData("librarycollection", collectionid);
		if (project == null)
		{
			log.warn("Project not found: " + collectionid);
			inReq.putPageValue("error", "Project not found");
		}
		return project;
	}

	public Set<String> getJoinedProjectIds(MediaArchive archive, String inUserid)
	{
		Collection<Data> rows = archive.query("librarycollectionusers").exact("followeruser", inUserid).exact("ontheteam", "true").search();
		Set<String> ids = new HashSet<String>();
		for (Data row : rows)
		{
			ids.add(row.get("collectionid"));
		}
		return ids;
	}
}
