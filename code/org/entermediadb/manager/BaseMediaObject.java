package org.entermediadb.manager;

import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;

public class BaseMediaObject implements CatalogEnabled
{
	protected String fieldCatalogId;

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String catalogId)
	{
		fieldCatalogId = catalogId;
	}

	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
	}

	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

}
