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
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.openedit.page.Page;
import org.openedit.util.DateStorageUtil;
import org.openedit.data.Searcher;
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

	/**
	 * Uploads the posted file as an asset and creates an entity asset for it, on a project
	 * (collectionid) or on the profile of a user (userid, default the signed-in user). Optional
	 * name and longdescription. Page values: entityasset (the new record's map).
	 */
	public void uploadEntityAsset(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		if (inReq.getUser() == null)
		{
			inReq.putPageValue("error", "Sign in to upload");
			return;
		}
		String collectionid = blankToNull(inReq.getRequestParameter("collectionid"));
		String profileid = null;
		String owner = collectionid;
		if (collectionid == null)
		{
			String userid = blankToNull(inReq.getRequestParameter("userid"));
			if (userid == null)
			{
				userid = inReq.getUser().getId();
			}
			Data profile = archive.query("emeprofile").exact("owner", userid).searchOne();
			if (profile == null)
			{
				inReq.putPageValue("error", "No EME profile found for user " + userid);
				return;
			}
			profileid = profile.getId();
			owner = profileid;
		}

		FileUpload command = (FileUpload) archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest upload = command.parseArguments(inReq);
		FileUploadItem item = upload.getFirstItem();
		if (item == null || item.getName() == null || item.getName().isEmpty())
		{
			inReq.putPageValue("error", "No file was uploaded");
			return;
		}

		Asset asset;
		try
		{
			String sourcepath = "entityassets/" + owner + "/" + item.getName();
			String path = ("/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath).replace("//", "/");
			upload.saveFileAs(item, path, inReq.getUser());
			Page saved = archive.getPageManager().getPage(path);
			AssetImporter importer = archive.getAssetImporter();
			asset = importer.createAssetFromPage(archive, false, inReq.getUser(), saved, null);
			importer.saveAsset(archive, inReq.getUser(), asset);
			archive.fireSharedMediaEvent("importing/assetscreated");
		}
		catch (Exception e)
		{
			log.error("Could not save uploaded entity asset file", e);
			inReq.putPageValue("error", "Could not save the file: " + e.getMessage());
			return;
		}

		Searcher searcher = archive.getSearcher("entityasset");
		Data entity = searcher.createNewData();
		String name = blankToNull(inReq.getRequestParameter("name"));
		entity.setName(name == null ? item.getName() : name);
		entity.setValue("longdescription", blankToNull(inReq.getRequestParameter("longdescription")));
		entity.setValue("primarymedia", asset.getId());
		entity.setValue("primaryimage", asset.getId());
		entity.setValue("entity_date", new Date());
		if (collectionid != null)
		{
			entity.setValue("librarycollection", collectionid);
		}
		else
		{
			entity.setValue("emeprofile", profileid);
		}
		searcher.saveData(entity, inReq.getUser());

		inReq.putPageValue("entityasset", toMap(archive, inReq, entity));
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
		asset.put("thumbstatus", previewState(image));
		asset.put("imageassetid", image == null ? null : image.getId());
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

	/**
	 * Conversion state of one asset, for cheap polling. Parameters: assetid, optional size (a
	 * preview name such as image200x200). Page values: assetid, previewstate (ready, pending or
	 * error), previewurl (once ready).
	 */
	public void loadPreviewStatus(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String assetid = blankToNull(inReq.getRequestParameter("assetid"));
		if (assetid == null)
		{
			inReq.putPageValue("error", "No assetid given");
			return;
		}
		Asset asset = archive.getAsset(assetid);
		if (asset == null)
		{
			inReq.putPageValue("error", "No asset " + assetid);
			return;
		}
		String size = blankToNull(inReq.getRequestParameter("size"));
		if (size == null || !size.matches("[a-z0-9]+"))
		{
			size = "image200x200";
		}
		String state = previewState(asset);
		inReq.putPageValue("assetid", assetid);
		inReq.putPageValue("previewstate", state);
		if ("ready".equals(state))
		{
			String siteroot = (String) inReq.getPageValue("siteroot");
			inReq.putPageValue("previewurl", (siteroot == null ? "" : siteroot) + archive.asLinkToPreview(asset, size));
		}
	}

	/** ready, error or pending (still being converted); null without an asset. */
	private String previewState(Asset inAsset)
	{
		if (inAsset == null)
		{
			return null;
		}
		if ("2".equals(inAsset.get("previewstatus")))
		{
			return "ready";
		}
		return "error".equals(inAsset.get("importstatus")) ? "error" : "pending";
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
