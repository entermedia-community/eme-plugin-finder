package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.users.Permissions;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;

/**
 * @deprecated not used. See AdminModule.loadCustomModulePermissions
 * @author shanti
 *
 */
public class AssetControlModule extends BaseMediaModule
{

	private static final Log log = LogFactory.getLog(AssetControlModule.class);

	/**
	 * This is a funny action that actually checks the permissions of the assets directory
	 * 
	 * @param inReq
	 * @return
	 * @throws Exception
	 */

	public boolean loadDownloadPermission(WebPageRequest inReq)
	{
		// look in the assets xconf and check those permissions
		MediaArchive archive = getMediaArchive(inReq);
		String sourcepath = archive.getSourcePathForPage(inReq);
		if (sourcepath != null)
		{
			Asset asset = archive.getAssetBySourcePath(sourcepath);
			if (asset == null)
			{
				log.error("Asset not found for sourcepath: " + sourcepath);
				return false;
			}
			Permissions permissions = inReq.getUserProfile().getPermissions();
			Data module = getMediaArchive(inReq).getCachedData("module", "asset");
			log.info("Module: " + module + " Asset: " + asset + " Sourcepath: " + sourcepath);
			boolean can = permissions.canEntity(module, asset, "download");
			log.info("Download permission for asset " + asset + ": " + can);
			return can;
		}
		else
		{
			log.error("No sourcepath passed in " + inReq);
		}
		// loadAssetCollectionPermissions(inReq);
		return false;
	}

	public Boolean canViewAsset(WebPageRequest inReq)
	{
		Asset asset = (Asset) inReq.getPageValue("asset");
		if (asset == null)
		{
			String assetid = inReq.findValue("assetid");
			if (assetid != null && assetid.contains("multi")) // Should be with a !?
			{
				asset = getAsset(inReq);
				// log.info("can view asset was checking :" + assetid);
				// log.info("found: " + asset);
			}
		}

		if (asset == null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			String ispublic = archive.getCatalogSettingValue("catalogassetviewispublic");
			if (Boolean.parseBoolean(ispublic))
			{
				return true;
			}
		}

		MediaArchive archive = getMediaArchive(inReq);

		if (asset == null)
		{
			String assetid = inReq.findValue("assetid"); // Why again?
			if (assetid != null)
			{
				asset = archive.getAsset(assetid);
			}
		}
		if (asset == null)
		{
			return false;
		}
		Data module = getMediaArchive(inReq).getCachedData("module", "asset");
		Boolean can = inReq.getUserProfile().getPermissions().isViewerFor(module, asset) ;
		return can;

	}

		public Boolean canEditAsset(WebPageRequest inReq)
	{
		Asset asset = (Asset) inReq.getPageValue("asset");
		MediaArchive archive = getMediaArchive(inReq);

		if (asset == null)
		{
			String assetid = inReq.findValue("assetid");
			if (assetid != null)
			{
				asset = archive.getAsset(assetid);

			}
		}
		if (asset == null)
		{
			return false;
		}
		Data module = getMediaArchive(inReq).getCachedData("module", "asset");
		Boolean can = inReq.getUserProfile().getPermissions().isEditorFor(module, asset) ;
		return can;
	}

}
