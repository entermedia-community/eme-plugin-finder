package org.entermediadb.fulservice.scl;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.fulservice.scl.SclClient.SclException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;

/**
 * Fetches the SCL contractor page, keeps the raw response on disk, parses it, and records one
 * fsejecucionfuente row per run (same lifecycle as BukImportManager). Fetch and parse are separate
 * steps: the raw file is saved before parsing, so a changed page layout never loses the capture.
 *
 * Phase 1: no rows are written to business entities yet (that mapping needs the real response shape).
 * The run records counts and headers only; row data stays in the restricted raw file.
 */
public class SclImportManager
{
	private static final Log log = LogFactory.getLog(SclImportManager.class);
	public static final String JOB_ID = "scl_sync";
	protected static final ReentrantLock LOCK = new ReentrantLock();

	protected final MediaArchive fieldArchive;
	protected SclClient fieldClient = new SclClient();

	public SclImportManager(MediaArchive inArchive)
	{
		fieldArchive = inArchive;
	}

	public void setClient(SclClient inClient)
	{
		fieldClient = inClient;
	}

	public static boolean isRunning()
	{
		return LOCK.isLocked();
	}

	public interface Progress
	{
		void log(String inType, String inMessage);
	}

	/** @return the closed fsejecucionfuente row, or null when another import already holds the lock */
	public Data runImport(String inRunType, String inAgentJobId, String inActor, Progress inProgress)
	{
		if (!LOCK.tryLock())
		{
			inProgress.log("warning", "Another SCL import is already running; skipping");
			return null;
		}
		try
		{
			return doImport(inRunType, inAgentJobId, inActor, inProgress);
		}
		finally
		{
			LOCK.unlock();
		}
	}

	/**
	 * Raw captures are the source of truth for every import and are never deleted by the importer.
	 * Layout: <webapp>/WEB-INF/data/<catalogid>/scl-raw/<yyyy-MM>/scl_tablero_<yyyyMMdd-HHmmss>_<runid>.json
	 */
	protected File rawFile(Date inStarted, String inRunId)
	{
		String month = new java.text.SimpleDateFormat("yyyy-MM").format(inStarted);
		String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(inStarted);
		String path = "/WEB-INF/data/" + fieldArchive.getCatalogId() + "/scl-raw/" + month;
		File dir = new File(fieldArchive.getPageManager().getRepository().getStub(path).getAbsolutePath());
		dir.mkdirs();
		return new File(dir, "scl_tablero_" + stamp + "_" + inRunId + ".json");
	}

	/** Writes to a temp file then renames, so a crash never leaves a half-written capture under the final name */
	protected static void writeAtomic(File inFile, byte[] inBody) throws java.io.IOException
	{
		java.nio.file.Path tmp = java.nio.file.Path.of(inFile.getPath() + ".part");
		Files.write(tmp, inBody);
		Files.move(tmp, inFile.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE);
	}

	protected Data doImport(String inRunType, String inAgentJobId, String inActor, Progress inProgress)
	{
		Searcher runs = fieldArchive.getSearcher("fsejecucionfuente");
		Searcher raws = fieldArchive.getSearcher("fsregistrocrudo");

		Date started = new Date();
		MultiValued run = (MultiValued) runs.createNewData();
		run.setId("scl-" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(started) + "-" + UUID.randomUUID().toString().substring(0, 6));
		run.setValue("fstenant", "fulservice");
		run.setValue("fssistemafuente", "scl");
		run.setValue("fstipoejecucion", inRunType);
		run.setValue("fsstartedat", started);
		run.setValue("fsestadoejecucion", "running");
		run.setValue("fsagentjob", inAgentJobId);
		run.setValue("fscorrelationid", run.getId());
		run.setValue("fstipoactor", inActor == null ? "agent" : "human");
		run.setValue("fsactorref", "sclSyncSkill");
		run.setValue("datecreated", started);
		runs.saveData(run);
		inProgress.log("info", "Started SCL " + inRunType + " import " + run.getId());

		long read = 0, written = 0, rejected = 0;
		String status = "success";
		String errorCode = null, errorDetail = null;
		try
		{
			// 1. fetch + keep the raw capture BEFORE interpreting it
			byte[] body = fieldClient.contratista();
			File file = rawFile(started, run.getId());
			writeAtomic(file, body);
			String hash = sha256(body);
			inProgress.log("info", "Captured " + body.length + " bytes (sha256 " + hash.substring(0, 12) + ")");

			// 2. parse offline from the same bytes
			SclParser.Table table = new SclParser().parseTablero(body);
			read = table.rows.size();
			inProgress.log("info", "Parsed " + read + " rows; headers: " + table.headers);

			// 3. redacted record of the capture (no row data: it holds RUTs)
			JSONObject safe = new JSONObject();
			safe.put("headers", new JSONArray() {{ addAll(table.headers); }});
			safe.put("rowCount", read);
			safe.put("file", file.getName());
			safe.put("path", file.getAbsolutePath());
			MultiValued raw = (MultiValued) raws.createNewData();
			raw.setValue("fstenant", "fulservice");
			raw.setValue("fssistemafuente", "scl");
			raw.setValue("fsejecucionfuente", run.getId());
			raw.setValue("fsrecordtype", "tablero_resumen");
			raw.setValue("fsexternalid", run.getId());
			raw.setValue("fspayloadjson", safe.toJSONString());
			raw.setValue("filehash", hash);
			raw.setValue("fsredacted", true);
			raw.setValue("fsingestedat", started);
			raw.setValue("datecreated", started);
			raws.saveData(raw);
			written = 1; // the capture record; row mapping to business entities comes once the shape is confirmed
		}
		catch (SclException ex)
		{
			status = "failed";
			errorCode = ex.getCode();
			errorDetail = ex.getMessage();
			inProgress.log("error", "Import stopped: " + ex.getCode() + " - " + ex.getMessage());
		}
		catch (Exception ex)
		{
			status = "failed";
			errorCode = "internal";
			errorDetail = ex.getClass().getSimpleName() + ": " + ex.getMessage();
			log.error("SCL import failed", ex);
			inProgress.log("error", "Import failed: " + errorDetail);
		}

		Date finished = new Date();
		run.setValue("fsfinishedat", finished);
		run.setValue("fssourceasof", finished);
		run.setValue("fsestadoejecucion", status);
		run.setValue("fsrecordsread", read);
		run.setValue("fsrecordswritten", written);
		run.setValue("fsrecordsrejected", rejected);
		run.setValue("fserrorcode", errorCode);
		run.setValue("fserrordetail", errorDetail);
		runs.saveData(run);
		inProgress.log("failed".equals(status) ? "error" : "success", "Finished: " + status + " (" + read + " rows read)");
		return run;
	}

	protected static String sha256(byte[] inBytes) throws Exception
	{
		StringBuilder sb = new StringBuilder();
		for (byte b : MessageDigest.getInstance("SHA-256").digest(inBytes))
		{
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
