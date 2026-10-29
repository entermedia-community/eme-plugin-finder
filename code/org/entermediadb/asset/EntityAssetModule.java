package org.entermediadb.asset;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;

/**
 * Entity assets (entityasset) for the app, matched by their emeprofile field or by their
 * librarycollection field. Failures set the "error" page value, which the .json page reports.
 */
public class EntityAssetModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(EntityAssetModule.class);

	/**
	 * The entity assets of a project (collectionid), else of a profile (userid, default the
	 * signed-in user, found through emeprofile.owner). Optional query filters by name. Searched with the request so the entities'
	 * own security applies. Page values: assets (maps), totalhits.
	 */
	public void loadEntityAssets(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = blankToNull(inReq.getRequestParameter("collectionid"));
		String profileid = null;
		if (collectionid == null)
		{
			String userid = blankToNull(inReq.getRequestParameter("userid"));
			if (userid == null && inReq.getUser() != null)
			{
				userid = inReq.getUser().getId();
			}
			if (userid == null)
			{
				inReq.putPageValue("error", "No user or project given");
				return;
			}
			// The entity assets point at the user's emeprofile record (emeprofile.owner = user id).
			Data profile = archive.query("emeprofile").exact("owner", userid).searchOne();
			if (profile == null)
			{
				inReq.putPageValue("error", "No EME profile found for user " + userid);
				return;
			}
			profileid = profile.getId();
		}

		QueryBuilder query = archive.query("entityasset");
		if (collectionid != null)
		{
			query.exact("librarycollection", collectionid);
		}
		else
		{
			query.exact("emeprofile", profileid);
		}
		String text = blankToNull(inReq.getRequestParameter("query"));
		if (text != null)
		{
			query.match("name", text);
		}
		query.sort("entity_dateDown");

		HitTracker hits;
		try
		{
			hits = query.search(inReq);
		}
		catch (Exception e)
		{
			log.error("Could not search entity assets", e);
			inReq.putPageValue("error", "Could not search entity assets: " + e.getMessage());
			return;
		}
		hits.setHitsPerPage(100);

		List<Map<String, Object>> assets = new ArrayList<Map<String, Object>>();
		for (Object hit : hits.getPageOfHits())
		{
			assets.add(toMap(archive, inReq, (Data) hit));
		}
		inReq.putPageValue("assets", assets);
		inReq.putPageValue("totalhits", hits.size());
	}

	private Map<String, Object> toMap(MediaArchive archive, WebPageRequest inReq, Data inHit)
	{
		String siteroot = (String) inReq.getPageValue("siteroot");
		siteroot = siteroot == null ? "" : siteroot;

		// The file behind the entity: its primary media, else its primary image.
		Asset media = loadAsset(archive, inHit.get("primarymedia"));
		if (media == null)
		{
			media = loadAsset(archive, inHit.get("primaryimage"));
		}
		Asset image = loadAsset(archive, inHit.get("primaryimage"));

		Map<String, Object> asset = new LinkedHashMap<String, Object>();
		asset.put("id", inHit.getId());
		asset.put("name", inHit.getName());
		asset.put("description", inHit.get("longdescription"));
		asset.put("date", iso(inHit.getValue("entity_date")));
		asset.put("sourcetype", inHit.get("entitysourcetype"));
		asset.put("imageurl", image == null ? null : siteroot + archive.asLinkToPreview(image, "image200x200"));
		asset.put("tags", tags(inHit));
		asset.put("emeprofile", inHit.get("emeprofile"));
		asset.put("collectionid", inHit.get("librarycollection"));
		if (media != null)
		{
			asset.put("uploaddate", iso(media.getValue("assetaddeddate")));
			asset.put("mediaurl", originalUrl(archive, siteroot, media));
			asset.put("mediatype", mediaType(archive, media));
			asset.put("filename", media.getName());
		}
		return asset;
	}

	private Asset loadAsset(MediaArchive archive, String inAssetId)
	{
		return inAssetId == null || inAssetId.isEmpty() ? null : archive.getAsset(inAssetId);
	}

	/** Link to the original file (preset 0), as the web download links are built. */
	private String originalUrl(MediaArchive archive, String inSiteRoot, Asset inMedia)
	{
		Data original = archive.getCachedData("convertpreset", "0");
		return original == null ? null : archive.asLinkToShare(inSiteRoot, inMedia, original);
	}

	/** image, video, pdf or other, for choosing a viewer. */
	private String mediaType(MediaArchive archive, Asset inMedia)
	{
		String format = inMedia.get("fileformat");
		if ("pdf".equalsIgnoreCase(format))
		{
			return "pdf";
		}
		String rendertype = archive.getMediaRenderType(format);
		if ("image".equals(rendertype) || "video".equals(rendertype))
		{
			return rendertype;
		}
		return "other";
	}

	private List<String> tags(Data inHit)
	{
		List<String> tags = new ArrayList<String>();
		if (inHit instanceof MultiValued)
		{
			Collection<String> values = ((MultiValued) inHit).getValues("keywords");
			if (values != null)
			{
				tags.addAll(values);
			}
		}
		return tags;
	}

	private String iso(Object inDate)
	{
		if (!(inDate instanceof Date))
		{
			return null;
		}
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format((Date) inDate);
	}

	private String blankToNull(String inValue)
	{
		return inValue == null || inValue.trim().isEmpty() ? null : inValue.trim();
	}
}
