package org.entermediadb.ai.transcriber;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.asset.convert.managers.AudioConversionManager;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.repository.RepositoryException;

public class TranscriberManager extends BaseAiManager
{

	private static final Log log = LogFactory.getLog(TranscriberManager.class);

	/** Extra time on top of the asset length before an inprogress track is considered abandoned */
	protected static final long STALE_GRACE_MS = 60L * 60L * 1000L;

	public void transcribeAssets(ScriptLogger inLog, Collection<Asset> inAssets)
	{
		Collection<MultiValued> toprocess = new ArrayList<MultiValued>();

		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
		{
			MultiValued inAsset = (MultiValued) iterator.next();

			String mediatype = getMediaArchive().getMediaRenderType(inAsset);

			if (!"video".equals(mediatype) && !"audio".equals(mediatype))
			{
				continue;
			}
			toprocess.add(inAsset);

		}

		if (toprocess.size() > 0)
		{
			inLog.headline("Transcribing " + toprocess.size() + " asset(s)");

			for (Iterator iterator = toprocess.iterator(); iterator.hasNext();)
			{
				MultiValued inAsset = (MultiValued) iterator.next();

				if (inAsset.getValue("length") == null)
				{
					/// Can't process if no lenght defined
					inAsset.setValue("llmerror", true);
					getMediaArchive().saveData("asset", inAsset);
					iterator.remove();
					inLog.info("Skiping Asset with no lenght defined: " + inAsset);
					continue;
				}

				inLog.info("Transcribing: " + inAsset);

				long starttime = System.currentTimeMillis();
				boolean ok = transcribeOneAsset(inLog, inAsset);
				long duration = (System.currentTimeMillis() - starttime) / 1000L;

				if (ok)
				{
					inLog.info("Transcribed successfully! Took: " + duration + " seconds");
				}

			}
		}

	}

	public boolean transcribeOneAsset(ScriptLogger inLog, MultiValued inAsset)
	{

		Searcher captionSearcher = getMediaArchive().getSearcher("videotrack");

		Data inTrack = captionSearcher.query().exact("assetid", inAsset.getId()).searchOne();

		if (inTrack != null)
		{
			String status = inTrack.get("transcribestatus");
			if ("complete".equals(status))
			{
				inLog.info("Asset already transcribed");
				return false;
			}
			if ("inprogress".equals(status))
			{
				if (!isStale(inTrack, inAsset))
				{
					inLog.info("Asset transcription already in progress");
					return false;
				}
				inLog.info("Restarting abandoned transcription for " + inAsset);
			}

		}
		if (inTrack == null)
		{
			inTrack = captionSearcher.createNewData();
			inTrack.setProperty("assetid", inAsset.getId());
			inTrack.setValue("length", inAsset.getValue("length"));
		}

		inTrack.setValue("requesteddate", new Date());
		inTrack.setValue("sourcelang", "en");

		try
		{
			inTrack.setValue("transcribestatus", "inprogress");
			captionSearcher.saveData(inTrack);

			transcribe(inAsset, inTrack);
			inTrack.setValue("transcribestatus", "complete");
			return true;
		}
		catch (Exception e)
		{
			inLog.error("Could not transcribe " + inAsset, e);
			inTrack.setValue("transcribestatus", "error");
			inAsset.setValue("llmerror", true); // So the skill drops it from later steps
			return false;
		}
		finally
		{
			inTrack.setValue("completeddate", new Date());
			captionSearcher.saveData(inTrack);
		}
	}

	/** An inprogress track is abandoned (for example after a restart) once it has run longer than the asset length plus an hour */
	protected boolean isStale(Data inTrack, MultiValued inAsset)
	{
		Date requested = (Date) inTrack.getValue("requesteddate");
		if (requested == null)
		{
			return true;
		}
		Object length = inAsset.getValue("length");
		long lengthms = length instanceof Number ? Math.round(((Number) length).doubleValue() * 1000d) : 0L;
		long running = System.currentTimeMillis() - requested.getTime();
		return running > lengthms + STALE_GRACE_MS;
	}

	public void transcribe(MultiValued inAsset, Data inTrack) throws RepositoryException, IOException
	{
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");

		TranscodeTools transcodetools = archive.getTranscodeTools();
		AudioConversionManager manager = (AudioConversionManager) transcodetools.getManagerByFileFormat("mp3");
		ConvertInstructions instructions = manager.createInstructions((Asset) inAsset, "audio.mp3");
		ContentItem item = manager.findInput(instructions);

		if (item == null)
		{
			item = archive.getOriginalContent(inAsset);
		}

		instructions.setInputFile(item);

		if (inAsset.getValue("length") == null)
		{
			log.info("Asset with no lenght, can't transcribe.");
			return;
		}

		double length = (Double) inAsset.getValue("length");

		Collection captions = new ArrayList();

		for (double timeoffset = 0; timeoffset < length; timeoffset += 300)
		{

			instructions.setProperty("timeoffset", String.valueOf(timeoffset));
			instructions.setProperty("duration", "300");
			instructions.setProperty("bitrate", "128");
			instructions.setProperty("resample", "48000");

			Page page = archive.getPageManager().getPage("/WEB-INF/temp/" + inAsset.getId() + "data.mp3");
			archive.getPageManager().removePage(page);

			ContentItem tempfile = page.getContentItem();

			instructions.setOutputFile(tempfile);

			ConvertResult result = manager.createOutput(instructions, true);
			if (!result.isOk())
			{
				throw new OpenEditException("Could not transcode audio");
			}
			try
			{

				JSONObject transcriptions = getTranscribedData(tempfile);

				if (transcriptions == null)
				{
					log.error("Transcriber server error");
					throw new OpenEditException("Transcriber server error");
				}

				String language = (String) transcriptions.get("language");
				if (language != null)
				{
					inTrack.setValue("sourcelang", language);
				}

				Number speakercount = (Number) transcriptions.get("num_speakers");
				if (speakercount != null)
				{
					inTrack.setValue("speakercount", speakercount.longValue());
				}

				JSONArray segments = (JSONArray) transcriptions.get("segments");

				for (Iterator iterator2 = segments.iterator(); iterator2.hasNext();)
				{
					Map cuemap = new HashMap();
					JSONObject transcription = (JSONObject) iterator2.next();

					// json-simple returns Long for whole numbers such as 0
					double start = ((Number) transcription.get("start")).doubleValue();
					double end = ((Number) transcription.get("end")).doubleValue();
					String text = (String) transcription.get("text");
					String speaker = (String) transcription.get("speaker");

					if (speaker == null)
					{
						speaker = "Unknown";
					}

					cuemap.put("cliplabel", text);
					cuemap.put("speaker", speaker);
					cuemap.put("timecodestart", Math.round((timeoffset + start) * 1000d));
					cuemap.put("timecodelength", Math.round((end - start) * 1000d));

					captions.add(cuemap);

				}
				inTrack.setValue("captions", captions);
				archive.getSearcher("videotrack").saveData(inTrack);

				log.info("Transcribed " + timeoffset + "s - " + Math.min(timeoffset + 300, length) + "s of " + inAsset);

			}
			catch (Exception e)
			{
				if (e instanceof OpenEditException)
				{
					throw (OpenEditException) e;
				}
				throw new OpenEditException(e);
			}
			finally
			{
				archive.getPageManager().removePage(page);
			}
  		}
	}


	public JSONObject getTranscribedData(ContentItem audio) throws FileNotFoundException, Exception
	{

		File audioFile = new File(audio.getAbsolutePath());
		if (!audioFile.exists())
		{
			throw new FileNotFoundException("File not found: " + audioFile);
		}
		LlmConnection connection = getMediaArchive().getLlmConnection("transcribe");

		Map headers = new HashMap();
		headers.put("Authorization", "Bearer " + connection.getApiKey());

		Map params = new HashMap();
		params.put("file", audioFile);

		log.info("Transcribing audio file: " + audio.getPath());
		LlmResponse resp = connection.callJson("/transcribe", headers, params);

		JSONObject result = (JSONObject) resp.getRawResponse();
		return result;

	}

	/*
	 * public JSONArray getTranscribedData_OLD(ContentItem audio) throws FileNotFoundException,
	 * Exception { String endpoint = getMediaArchive().getCatalogSettingValue("ai_transcriber_server") +
	 * "/transcribe";
	 * 
	 * HttpPost method = new HttpPost(endpoint); method.addHeader("Authorization",
	 * "Bearer YOUR_SECRET_TOKEN");
	 * 
	 * File audioFile = new File(audio.getAbsolutePath()); if(!audioFile.exists()) { throw new
	 * FileNotFoundException("File not found: " + audioFile); }
	 * 
	 * HttpEntity entity = MultipartEntityBuilder.create() .addBinaryBody("file", audioFile,
	 * ContentType.create("audio/mp3"), audioFile.getName()) .build();
	 * 
	 * method.setEntity(entity);
	 * 
	 * CloseableHttpResponse resp = getSharedConnection().sharedExecute(method);
	 * 
	 * 
	 * if (resp.getStatusLine().getStatusCode() != 200) { log.info("Transcriber server error returned "
	 * + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase()); String
	 * returned = EntityUtils.toString(resp.getEntity()); log.info(returned); return null; }
	 * 
	 * else { String returned = EntityUtils.toString(resp.getEntity()); JSONArray result = (JSONArray)
	 * new JSONParser().parseMapArray(returned); return result;
	 * 
	 * } }
	 */
}
