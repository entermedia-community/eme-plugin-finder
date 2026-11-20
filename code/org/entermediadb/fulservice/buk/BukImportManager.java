package org.entermediadb.fulservice.buk;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.fulservice.buk.BukClient.BukException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;

/**
 * Imports BUK employees into fstrabajador, one fsejecucionfuente row per run.
 * Only a whitelist of non-sensitive fields is stored (no RUT, wage, bank, health, family or contact data).
 * Overlap between a scheduled run and an "import now" is prevented by a single in-JVM lock.
 */
public class BukImportManager
{
	private static final Log log = LogFactory.getLog(BukImportManager.class);
	public static final String JOB_ID = "buk_sync";
	protected static final ReentrantLock LOCK = new ReentrantLock();

	protected final MediaArchive fieldArchive;
	protected BukClient fieldClient = new BukClient();

	public BukImportManager(MediaArchive inArchive)
	{
		fieldArchive = inArchive;
	}

	public void setClient(BukClient inClient)
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
			inProgress.log("warning", "Another BUK import is already running; skipping");
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

	protected Data doImport(String inRunType, String inAgentJobId, String inActor, Progress inProgress)
	{
		Searcher runs = fieldArchive.getSearcher("fsejecucionfuente");
		Searcher workers = fieldArchive.getSearcher("fstrabajador");
		Searcher raws = fieldArchive.getSearcher("fsregistrocrudo");
		DocStats docs = new DocStats();

		Date started = new Date();
		MultiValued run = (MultiValued) runs.createNewData();
		run.setId("buk-" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(started) + "-" + UUID.randomUUID().toString().substring(0, 6));
		run.setValue("fstenant", "fulservice");
		run.setValue("fssistemafuente", "buk");
		run.setValue("fstipoejecucion", inRunType);
		run.setValue("fsstartedat", started);
		run.setValue("fsestadoejecucion", "running");
		run.setValue("fsagentjob", inAgentJobId);
		run.setValue("fscorrelationid", run.getId());
		run.setValue("fstipoactor", inActor == null ? "agent" : "human");
		run.setValue("fsactorref", "bukSyncSkill");
		run.setValue("datecreated", started);
		runs.saveData(run);
		inProgress.log("info", "Started BUK " + inRunType + " import " + run.getId());

		long read = 0, written = 0, rejected = 0, skipped = 0;
		String status = "success";
		String errorCode = null, errorDetail = null;
		try
		{
			int page = 1;
			while (true)
			{
				JSONObject body = fieldClient.employees(page);
				JSONArray data = (JSONArray) body.get("data");
				if (data == null || data.isEmpty())
				{
					break;
				}
				for (Object o : data)
				{
					read++;
					try
					{
						JSONObject emp = (JSONObject) o;
						if (upsert(workers, raws, run, emp, started))
						{
							written++;
						}
						else
						{
							skipped++;
						}
						importDocuments(emp.get("id"), run, started, docs, inProgress);
					}
					catch (Exception ex)
					{
						rejected++;
						log.error("Rejected BUK record: " + ex.getMessage());
					}
				}
				inProgress.log("info", "Page " + page + " imported: " + read + " read, " + written + " saved, " + skipped + " unchanged, " + rejected + " rejected; documents: " + docs.summary());
				if (data.size() < BukClient.PAGE_SIZE)
				{
					break;
				}
				page++;
			}
			if (rejected > 0)
			{
				status = "partial";
			}
		}
		catch (BukException ex)
		{
			status = (written > 0) ? "partial" : "failed";
			errorCode = ex.getCode();
			errorDetail = ex.getMessage();
			inProgress.log("error", "Import stopped: " + ex.getCode() + " - " + ex.getMessage());
		}
		catch (Exception ex)
		{
			status = (written > 0) ? "partial" : "failed";
			errorCode = "internal";
			errorDetail = ex.getClass().getSimpleName() + ": " + ex.getMessage();
			log.error("BUK import failed", ex);
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
		inProgress.log("failed".equals(status) ? "error" : "success", "Finished: " + status + " (" + read + " read, " + written + " saved, " + skipped + " unchanged, " + rejected + " rejected; documents: " + docs.summary() + ")");
		return run;
	}

	/** @return false when the employee is already stored with an identical redacted record (nothing written) */
	protected boolean upsert(Searcher inWorkers, Searcher inRaws, MultiValued inRun, JSONObject inEmp, Date inNow)
	{
		Object extid = inEmp.get("id");
		if (extid == null)
		{
			throw new IllegalArgumentException("record without id");
		}
		String id = "buk-" + extid;
		String first = str(inEmp.get("first_name"));
		String last = str(inEmp.get("surname"));
		String name = (first + " " + last).trim();
		JSONObject job = inEmp.get("current_job") instanceof JSONObject ? (JSONObject) inEmp.get("current_job") : new JSONObject();

		// Redacted copy only: whitelist, never the raw payload
		JSONObject safe = new JSONObject();
		safe.put("id", extid);
		safe.put("name", name);
		safe.put("status", inEmp.get("status"));
		safe.put("role", job.get("role") instanceof JSONObject ? ((JSONObject) job.get("role")).get("name") : job.get("role_name"));
		safe.put("cost_center", job.get("cost_center"));
		safe.put("ficha", inEmp.get("code_sheet"));
		safe.put("start_date", job.get("start_date"));
		safe.put("end_date", job.get("end_date"));
		String payload = safe.toJSONString();
		String hash = sha256(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

		MultiValued worker = (MultiValued) inWorkers.searchById(id);
		boolean isnew = worker == null;
		if (!isnew && inRaws.query().exact("fssistemafuente", "buk").exact("fsrecordtype", "employee").exact("fsexternalid", String.valueOf(extid)).exact("filehash", hash).searchOne() != null)
		{
			return false; // same data as a previous run: do not re-import
		}
		if (isnew)
		{
			worker = (MultiValued) inWorkers.createNewData();
			worker.setId(id);
			worker.setValue("datecreated", inNow);
		}
		worker.setValue("fstenant", "fulservice");
		worker.setValue("name", name.isEmpty() ? id : name);
		worker.setValue("fsbukemployeeid", String.valueOf(extid));
		worker.setValue("fsestadotrabajador", mapStatus(str(inEmp.get("status"))));
		worker.setValue("fsactivesince", parseDate(job.get("start_date")));
		worker.setValue("fsactiveuntil", parseDate(job.get("end_date")));
		worker.setValue("fssourceasof", inNow);
		worker.setValue("datemodified", inNow);
		inWorkers.saveData(worker);

		MultiValued raw = (MultiValued) inRaws.createNewData();
		raw.setValue("fstenant", "fulservice");
		raw.setValue("fssistemafuente", "buk");
		raw.setValue("fsejecucionfuente", inRun.getId());
		raw.setValue("fsrecordtype", "employee");
		raw.setValue("fsexternalid", String.valueOf(extid));
		raw.setValue("fspayloadjson", payload);
		raw.setValue("filehash", hash);
		raw.setValue("fsredacted", true);
		raw.setValue("fsingestedat", inNow);
		raw.setValue("datecreated", inNow);
		inRaws.saveData(raw);
		worker.setValue("fsregistrocrudo", raw.getId());
		inWorkers.saveData(worker);
		return true;
	}

	protected static class DocStats
	{
		long listed, downloaded, replaced, existing, notThisMonth, failed;
		boolean disabled; // token has no documents access: stop asking

		String summary()
		{
			return downloaded + " new, " + replaced + " replaced, " + existing + " already stored, " + notThisMonth + " older, " + failed + " failed" + (disabled ? " (documents access denied, skipped)" : "");
		}
	}

	/** Root folder for downloaded documents. Override with the FULSERVICE_BUK_DOCS_DIR environment variable. */
	public static final String DOCS_DIR_ENV = "FULSERVICE_BUK_DOCS_DIR";

	protected File docsRoot()
	{
		String env = System.getenv(DOCS_DIR_ENV);
		if (env != null && !env.isBlank())
		{
			return new File(env);
		}
		return new File(fieldArchive.getPageManager().getRepository().getStub("/WEB-INF/data/" + fieldArchive.getCatalogId() + "/buk-docs").getAbsolutePath());
	}

	/**
	 * Downloads the files BUK uploaded this month (the month of the run) for one employee, to
	 * <root>/<employeeId>/<fileId>.<ext>. A file already stored with the same created date is skipped; a changed created date replaces it.
	 * The original filename is not kept: it holds the worker's name. No entities are created.
	 */
	protected void importDocuments(Object inEmployeeId, MultiValued inRun, Date inNow, DocStats inStats, Progress inProgress)
	{
		if (inStats.disabled || inEmployeeId == null)
		{
			return;
		}
		String month = new SimpleDateFormat("yyyy-MM").format(inNow);
		try
		{
			Object list = fieldClient.docs(inEmployeeId).get("employee_files");
			if (!(list instanceof JSONArray))
			{
				return;
			}
			for (Object o : (JSONArray) list)
			{
				JSONObject f = (JSONObject) o;
				Object fileId = f.get("file_id");
				Date created = parseBukDateTime(f.get("created_at"));
				inStats.listed++;
				if (fileId == null || created == null || !month.equals(new SimpleDateFormat("yyyy-MM").format(created)))
				{
					inStats.notThisMonth++;
					continue;
				}
				String filename = str(f.get("filename"));
				String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).replaceAll("[^A-Za-z0-9]", "").toLowerCase() : "bin";
				File dir = new File(docsRoot(), String.valueOf(inEmployeeId));
				File file = new File(dir, fileId + "." + ext);
				// The file's modified time is set to BUK's created_at, so an equal time means "same document, already stored"
				if (file.exists() && file.lastModified() == created.getTime())
				{
					inStats.existing++;
					continue;
				}
				boolean replacing = file.exists();
				try
				{
					byte[] body = fieldClient.downloadDoc(inEmployeeId, fileId);
					dir.mkdirs();
					java.nio.file.Path tmp = java.nio.file.Path.of(file.getPath() + ".part");
					Files.write(tmp, body);
					Files.move(tmp, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					file.setLastModified(created.getTime());
					// Same document id saved earlier under another extension: the latest one replaces it
					File[] older = dir.listFiles((d, n) -> n.startsWith(fileId + ".") && !n.equals(file.getName()) && !n.endsWith(".part"));
					if (older != null)
					{
						for (File x : older)
						{
							x.delete();
							replacing = true;
						}
					}
					if (replacing)
					{
						inStats.replaced++;
					}
					else
					{
						inStats.downloaded++;
					}
				}
				catch (BukException ex)
				{
					inStats.failed++;
					log.error("BUK document download failed (" + ex.getCode() + ")");
				}
			}
		}
		catch (BukException ex)
		{
			if ("http_403".equals(ex.getCode()) || "http_401".equals(ex.getCode()))
			{
				inStats.disabled = true;
				inProgress.log("warning", "BUK token cannot read documents (" + ex.getCode() + "); skipping documents for this run");
			}
			else if (!"http_404".equals(ex.getCode()))
			{
				inStats.failed++;
				log.error("BUK document list failed (" + ex.getCode() + ")");
			}
		}
		catch (Exception ex)
		{
			inStats.failed++;
			log.error("BUK document import error: " + ex.getClass().getSimpleName());
		}
	}

	/** BUK file timestamps look like "27/03/23 11:28" */
	protected static Date parseBukDateTime(Object o)
	{
		if (o == null)
		{
			return null;
		}
		try
		{
			SimpleDateFormat f = new SimpleDateFormat("dd/MM/yy HH:mm");
			f.setLenient(false);
			return f.parse(String.valueOf(o));
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	protected static String sha256(byte[] inBytes)
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			for (byte b : MessageDigest.getInstance("SHA-256").digest(inBytes))
			{
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception ex)
		{
			throw new IllegalStateException(ex);
		}
	}

	protected static String str(Object o)
	{
		return o == null ? "" : String.valueOf(o);
	}

	protected static String mapStatus(String inBuk)
	{
		switch (inBuk.toLowerCase())
		{
			case "activo":
			case "active":
				return "active";
			case "inactivo":
			case "inactive":
				return "inactive";
			case "pendiente":
			case "pending":
				return "pending";
			case "desvinculado":
			case "terminated":
				return "terminated";
			default:
				return "unknown";
		}
	}

	protected static Date parseDate(Object o)
	{
		if (o == null || String.valueOf(o).isBlank())
		{
			return null;
		}
		try
		{
			return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(String.valueOf(o));
		}
		catch (Exception ex)
		{
			return null;
		}
	}
}
