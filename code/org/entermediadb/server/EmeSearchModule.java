package org.entermediadb.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.projects.ProjectManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;

/**
 * One search across everything the app's Search screen shows: people, projects, EME servers and
 * events. It runs the same queries as the per-type services (usersearch,
 * ProjectManager.searchOpenProjects, EmeServerModule.getEMEServers, emeevent/events) and puts each
 * result list on the page for search.json.
 *
 * Request parameters: query (name filter, optional), types (comma separated subset of people,
 * projects, servers, events; default all) and category (an EME server category id). Projects and
 * events have no category, so a category leaves them out.
 */
public class EmeSearchModule extends BaseMediaModule
{
	private static final Set<String> ALL_TYPES = new HashSet<String>(Arrays.asList("people", "projects", "servers", "events"));

	private EmeServerModule fieldEmeServerModule;

	public EmeServerModule getEmeServerModule()
	{
		return fieldEmeServerModule;
	}

	public void setEmeServerModule(EmeServerModule inEmeServerModule)
	{
		fieldEmeServerModule = inEmeServerModule;
	}

	/**
	 * Page values: types (the searched types), users, projects (hits) with joinedids, emeservers and
	 * events. A type that was not searched is absent.
	 */
	public void search(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		User user = inReq.getUser();
		String query = inReq.getRequestParameter("query");
		if (query != null)
		{
			query = query.trim();
		}
		String category = inReq.getRequestParameter("category");
		boolean hasCategory = category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("all");

		Set<String> types = parseTypes(inReq.getRequestParameter("types"));
		if (hasCategory)
		{
			types.remove("projects");
			types.remove("events");
		}
		inReq.putPageValue("types", types);

		if (types.contains("people"))
		{
			inReq.putPageValue("users", searchUsers(archive, inReq, query));
		}
		if (types.contains("projects"))
		{
			ProjectManager projects = (ProjectManager) getModuleManager().getBean(archive.getCatalogId(), "projectManager");
			inReq.putPageValue("projects", projects.searchOpenProjects(inReq, query));
			inReq.putPageValue("joinedids", projects.getJoinedProjectIds(inReq.getUserName()));
		}
		if (types.contains("servers"))
		{
			inReq.putPageValue("emeservers", getEmeServerModule().searchServers(archive, user.getId(), query, hasCategory ? category : null));
		}
		if (types.contains("events"))
		{
			inReq.putPageValue("events", searchEvents(archive, user.getId(), query));
		}
	}

	private Set<String> parseTypes(String inTypes)
	{
		Set<String> types = new HashSet<String>();
		if (inTypes != null)
		{
			for (String type : inTypes.split(","))
			{
				type = type.trim().toLowerCase();
				if (ALL_TYPES.contains(type))
				{
					types.add(type);
				}
			}
		}
		if (types.isEmpty())
		{
			types.addAll(ALL_TYPES);
		}
		return types;
	}

	/** Users whose description starts with the query, as usersearch does; every user when empty. */
	private HitTracker searchUsers(MediaArchive archive, WebPageRequest inReq, String inQuery)
	{
		Searcher userSearcher = getSearcherManager().getSearcher("system", "user");
		QueryBuilder builder = userSearcher.query();
		if (inQuery == null || inQuery.isEmpty())
		{
			builder.all();
		}
		else
		{
			builder.startsWith("description", inQuery);
		}
		HitTracker hits = builder.search(inReq);
		hits.setHitsPerPage(100);
		return hits;
	}

	/**
	 * Events whose name matches the query (all when empty), newest start first, each with its rsvp
	 * counts and the user's own response, as emeevent/events.
	 */
	private List<Map> searchEvents(MediaArchive archive, String inUserId, String inQuery)
	{
		QueryBuilder builder = archive.query("collectiveevent");
		if (inQuery == null || inQuery.isEmpty())
		{
			builder.all();
		}
		else
		{
			builder.match("name", inQuery);
		}
		Collection<MultiValued> hits = builder.sort("startdateDown").search();

		List<Map> events = new ArrayList<Map>();
		for (MultiValued hit : hits)
		{
			Map event = new LinkedHashMap();
			event.put("id", hit.getId());
			event.put("name", hit.getName());
			event.put("description", hit.get("eventdescription"));
			event.put("location", hit.get("eventlocation"));
			event.put("imageurl", hit.get("eventimageurl"));
			event.put("startdate", iso(hit.getDate("startdate")));
			event.put("enddate", iso(hit.getDate("enddate")));
			String hostid = hit.get("eventhost");
			event.put("hostid", hostid);
			User host = hostid == null ? null : archive.getUser(hostid);
			event.put("hostname", host == null ? null : host.getScreenName());

			int going = 0;
			int interested = 0;
			String mine = null;
			Collection<MultiValued> rsvps = archive.query("collectiveeventrsvp").exact("eventid", hit.getId()).search();
			for (MultiValued rsvp : rsvps)
			{
				String status = rsvp.get("rsvpstatus");
				if ("going".equals(status))
				{
					going++;
				}
				else if ("interested".equals(status))
				{
					interested++;
				}
				if (inUserId != null && inUserId.equals(rsvp.get("rsvpuser")))
				{
					mine = status;
				}
			}
			event.put("goingcount", going);
			event.put("interestedcount", interested);
			event.put("myrsvp", mine);
			events.add(event);
		}
		return events;
	}

	private String iso(Date inDate)
	{
		if (inDate == null)
		{
			return null;
		}
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(inDate);
	}
}
