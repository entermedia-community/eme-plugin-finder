package org.entermediadb.asset.push;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;

public interface MediaUploader
{

	public boolean uploadOriginal(MediaArchive archive, Asset inAsset, Data inPublishDestination);

	public boolean uploadGenerated(MediaArchive archive, Asset inAsset, Data inPublishDestination);

}
