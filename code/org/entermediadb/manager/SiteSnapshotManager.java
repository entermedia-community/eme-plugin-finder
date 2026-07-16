package org.entermediadb.manager;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Requests;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.util.CSVReader;
import org.entermediadb.asset.util.ImportFile;
import org.entermediadb.asset.util.Row;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.elasticsearch.searchers.ElasticListSearcher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.node.NodeManager;
import org.openedit.page.Page;
import org.openedit.page.PageSettings;
import org.openedit.page.manage.PageManager;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;
import org.openedit.util.XmlUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class SiteSnapshotManager extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(SiteSnapshotManager.class);

	public void restoreSnapshot(WebPageRequest inReq, Data snapshot) throws Exception
	{
		log.info("Initializing restore");
		SearcherManager searcherManager = (SearcherManager) inReq.getPageValue("searcherManager");
		Searcher snapshotsearcher = searcherManager.getSearcher("system", "sitesnapshot");

		snapshot.setValue("snapshotstatus", "restoring");
		snapshotsearcher.saveData(snapshot);

		Searcher sitesearcher = searcherManager.getSearcher("system", "site");
		Data site = sitesearcher.query().match("id", snapshot.get("site")).searchOne();

		String catalogid = site.get("catalogid");
		MediaArchive mediaarchive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");

		try
		{
			Boolean configonly = (Boolean) snapshot.getValue("configonly");
			if (configonly == null)
			{
				configonly = false;
			}

			String logstring = String.format("restoring: %s config= %s ", site.get("rootpath"), configonly);
			log.info(logstring);

			restore(mediaarchive, site, snapshot, configonly);
			snapshot.setValue("snapshotstatus", "complete");
		}
		catch (Exception ex)
		{
			log.error("Could not restore", ex);
			snapshot.setValue("snapshotstatus", "error");
		}
		finally
		{
			mediaarchive.getSearcherManager().resetAlternative();
		}
		snapshotsearcher.saveData(snapshot);
		mediaarchive.getSearcherManager().clear();

		log.info("Snapshot restore finished.");

	}

	public void restore(MediaArchive mediaarchive, Data site, Data inSnap, boolean configonly) throws Exception
	{
		String folder = inSnap.get("folder");
		String catalogid = mediaarchive.getCatalogId();
		String rootfolder = "/WEB-INF/data/exports/" + mediaarchive.getCatalogId() + "/" + folder;

		Collection<String> files = mediaarchive.getPageManager().getChildrenPaths(rootfolder);
		if (files.isEmpty())
		{
			throw new OpenEditException("No files in " + rootfolder);
		}

		Date date = new Date();
		ElasticNodeManager nodeManager = (ElasticNodeManager) mediaarchive.getNodeManager();
		String tempindex = nodeManager.toId(mediaarchive.getCatalogId().replaceAll("_", "") + date.getTime());

		Page lists = mediaarchive.getPageManager().getPage(rootfolder + "/lists/");
		if (lists.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
			archiveFolder(mediaarchive.getPageManager(), target, tempindex);
			mediaarchive.getPageManager().copyPage(lists, target);
		}

		Page views = mediaarchive.getPageManager().getPage(rootfolder + "/views/");
		if (views.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/views/");
			archiveFolder(mediaarchive.getPageManager(), target, tempindex);
			mediaarchive.getPageManager().copyPage(views, target);
		}

		Page orig = mediaarchive.getPageManager().getPage(rootfolder + "/originals");
		if (orig.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/originals/");
			mediaarchive.getPageManager().copyPage(orig, target);
		}

		Page gen = mediaarchive.getPageManager().getPage(rootfolder + "/generated");
		if (gen.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated/");
			mediaarchive.getPageManager().copyPage(gen, target);
		}

		mediaarchive.getPageManager().clearCache();
		log.info("Clearing property definitions");
		PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();

		Page fields = mediaarchive.getPageManager().getPage(rootfolder + "/fields/");
		if (fields.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/");
			archiveFolder(mediaarchive.getPageManager(), target, tempindex);
			mediaarchive.getPageManager().copyPage(fields, target);
		}
		pdarchive.clearCache();

		if (!configonly)
		{
			log.info("Preparing index " + tempindex);
			nodeManager.prepareIndex(tempindex);
			log.info("Index " + tempindex + " prepared");
		}

		if (!configonly)
		{
			List<String> orderedtypes = new ArrayList<>();
			orderedtypes.add("category");

			List<String> childrennames = pdarchive.findChildTablesNames();

			List<String> jsonfiles = pdarchive.getPageManager().getChildrenPaths(rootfolder + "/json/");
			List<String> mappings = new ArrayList<>();
			List<String> orderedJsontypes = new ArrayList<>();

			for (String it : jsonfiles)
			{
				if (it.endsWith(".zip"))
				{
					String searchtype = PathUtilities.extractPageName(it);
					if (!childrennames.contains(searchtype))
					{
						orderedJsontypes.add(searchtype);
					}
				}
				if (it.endsWith(".json"))
				{
					String filename = PathUtilities.extractPageName(it);
					mappings.add(filename);
				}
			}

			orderedJsontypes.addAll(childrennames);
			orderedJsontypes.remove("propertydetail");
			orderedJsontypes.remove("lock");
			orderedJsontypes.remove("user");
			orderedJsontypes.remove("group");

			for (String it : mappings)
			{
				Page upload = mediaarchive.getPageManager().getPage(rootfolder + "/json/" + it + ".json");
				String searchtype = it.substring(0, it.indexOf("-"));
				log.info("Restore - Put Mappings: " + searchtype);
				putMapping(mediaarchive, searchtype, upload, tempindex);
			}

			/*
			 * Searcher categories = mediaarchive.getSearcher("category");
			 * categories.setAlternativeIndex(tempindex); log.info("Restore - Put Mappings: category");
			 * categories.putMappings(); categories.setAlternativeIndex(null);
			 */
			log.info("Importing Data for " + orderedJsontypes.size() + " types");
			log.info(orderedJsontypes);
			for (String type : orderedJsontypes)
			{
				Page upload = mediaarchive.getPageManager().getPage(rootfolder + "/json/" + type + ".zip");
				try
				{
					if (upload.exists())
					{
						log.info("Restore - Importing: " + type);
						importJson(site, mediaarchive, type, upload, tempindex);
					}
				}
				catch (Exception e)
				{
					log.error("Exception thrown importing upload: " + upload, e);
					break;
				}

			}
			log.info("Import Data completed");
		}
	}

	public void archiveFolder(PageManager inManager, Page inPage, String inIndex)
	{
		if (inPage.exists() && !"false".equals(inPage.get("cleanonimport")))
		{
			Page trash = inManager.getPage("/WEB-INF/trash/" + inIndex + inPage.getPath());
			inManager.movePage(inPage, trash);
		}
	}

	public void importCsv(Data site, MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception
	{
		Boolean fastmode = Boolean.parseBoolean(mediaarchive.getPageManager().getPage("/WEB-INF/data/system/configuration/testimportmode.xml").get("testimportmode"));

		log.info("Importing data " + upload.getPath());
		Row trow = null;
		ArrayList<Data> tosave = new ArrayList<>();
		String catalogid = mediaarchive.getCatalogId();

		Reader reader = upload.getReader();
		ImportFile file = new ImportFile();
		file.setParser(new CSVReader(reader, ',', '\"'));
		file.read(reader);

		PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();
		PropertyDetails details = pdarchive.getPropertyDetails(searchtype);

		Searcher searcher = mediaarchive.getSearcher(searchtype);
		details = searcher.getPropertyDetails();
		searcher.setAlternativeIndex(tempindex);
		if (!searcher.putMappings())
		{
			throw new OpenEditException("Could not define dynamic or static fields, check mapping errors");
		}

		int count = 0;
		searcher.setForceBulk(true);
		while ((trow = file.getNextRow()) != null && ((fastmode && count < 1000) || !fastmode))
		{
			count++;
			String id = trow.get("id");
			Data newdata = searcher.createNewData();
			newdata.setId(id);

			for (Iterator<String> iterator = file.getHeader().getHeaderNames().iterator(); iterator.hasNext();)
			{
				String header = iterator.next();
				String detailid = header;
				String value = trow.get(header);

				if (detailid != null && detailid.contains("."))
				{
					continue;
				}

				PropertyDetail detail = details.getDetail(detailid);
				if (detail == null)
				{
					for (PropertyDetail pd : details)
					{
						String legacy = pd.get("legacy");
						if (legacy != null && legacy.equals(header))
						{
							detail = pd;
							break;
						}
					}
				}

				if (header.contains("."))
				{
					String[] splits = header.split("\\.");
					if (splits.length > 1)
					{
						detail = searcher.getDetail(splits[0]);
						if (detail != null && detail.isMultiLanguage())
						{
							LanguageMap map = null;
							Object values = newdata.getValue(detail.getId());
							if (values instanceof LanguageMap)
							{
								map = (LanguageMap) values;
							}
							if (values instanceof String)
							{
								map = new LanguageMap();
								map.put("en", (String) values);
							}
							if (map == null)
							{
								map = new LanguageMap();
							}
							map.put(splits[1], value);
							newdata.setValue(detail.getId(), map);
						}
					}
					continue;
				}

				if (detail == null)
				{
					continue;
				}
				if (value == null)
				{
					continue;
				}

				if (detail.isDate())
				{
					try
					{
						Date date = DateStorageUtil.getStorageUtil().parseFromStorage(value);
						newdata.setValue(detail.getId(), date);
					}
					catch (Exception e)
					{
						log.error("Parse issue " + value);
					}
				}
				else
				{
					if ("app".equals(searchtype) && "deploypath".equals(detail.getId()))
					{
						int inx = value.indexOf("/");
						if (inx > 1)
						{
							value = site.get("rootpath") + value.substring(inx - 1);
						}
					}
					newdata.setValue(detail.getId(), value);
				}
			}

			tosave.add(newdata);

			if (tosave.size() > 10000)
			{
				searcher.saveAllData(tosave, null);
				tosave.clear();
			}
		}

		searcher.saveAllData(tosave, null);
		searcher.setAlternativeIndex(null);
		FileUtils.safeClose(reader);
		searcher.setForceBulk(false);
		searcher.setAlternativeIndex(null);
		searcher.clearIndex();
		log.info("Saved " + searchtype + " " + tosave.size());
	}

	public void importJson(Data site, MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception
	{
		Searcher searcher = mediaarchive.getSearcher(searchtype);
		if (searcher instanceof ElasticListSearcher)
		{
			return;
		}

		ElasticNodeManager manager = (ElasticNodeManager) mediaarchive.getNodeManager();
		BulkProcessor processor = manager.getBulkProcessor();
		int count = 0;
		try
		{
			ZipInputStream unzip = new ZipInputStream(upload.getInputStream());
			ZipEntry entry = unzip.getNextEntry();

			MappingJsonFactory f = new MappingJsonFactory();
			JsonParser jp = f.createParser(new InputStreamReader(unzip, "UTF-8"));

			JsonToken current;
			current = jp.nextToken();
			if (current != JsonToken.START_OBJECT)
			{
				System.out.println("Error: root should be object: quiting.");
				return;
			}

			while (jp.nextToken() != JsonToken.END_OBJECT)
			{
				String fieldName = jp.getCurrentName();
				current = jp.nextToken();
				if (fieldName.equals(searchtype))
				{
					if (current == JsonToken.START_ARRAY)
					{
						while (jp.nextToken() != JsonToken.END_ARRAY)
						{
							JsonNode node = jp.readValueAsTree();
							IndexRequest req = Requests.indexRequest(tempindex).type(searchtype);
							JsonNode source = node.get("_source");
							if (source == null)
							{
								source = node;
							}
							String json = source.toString();
							req.source(json);

							JsonNode idNode = node.get("_id");
							if (idNode == null)
							{
								idNode = node.get("id");
							}
							if (idNode == null)
							{
								log.info("No ID found " + searchtype + " node:" + node);
							}
							else
							{
								req.id(idNode.asText());
							}
							processor.add(req);
							count++;
						}
					}
					else
					{
						System.out.println("Error: records should be an array: skipping.");
						jp.skipChildren();
					}
				}
				else
				{
					System.out.println("Unprocessed property: " + fieldName);
					jp.skipChildren();
				}

				if (count > 0 && count % 10000 == 0)
				{
					log.info("Importing: " + count + " records for " + searchtype);
				}
			}
		}
		finally
		{
			manager.flushBulk();
			log.info("Imported: " + searchtype + " " + count + " records");
		}
	}

	public void putMapping(MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception
	{
		ElasticNodeManager manager = (ElasticNodeManager) mediaarchive.getNodeManager();
		AdminClient admin = manager.getClient().admin();
		PutMappingRequest req = Requests.putMappingRequest(tempindex).updateAllTypes(true).type(searchtype);
		req = req.source(upload.getContent());
		req.validate();
		PutMappingResponse pres = admin.indices().putMapping(req).actionGet();
	}
}
