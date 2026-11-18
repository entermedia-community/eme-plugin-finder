package org.entermediadb.fulservice.buk;

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

		long read = 0, written = 0, rejected = 0;
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
						upsert(workers, raws, run, (JSONObject) o, started);
						written++;
					}
					catch (Exception ex)
					{
						rejected++;
						log.error("Rejected BUK record: " + ex.getMessage());
					}
				}
				inProgress.log("info", "Page " + page + " imported: " + read + " read, " + written + " saved, " + rejected + " rejected");
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
		inProgress.log("failed".equals(status) ? "error" : "success", "Finished: " + status + " (" + read + " read, " + written + " saved, " + rejected + " rejected)");
		return run;
	}

	protected void upsert(Searcher inWorkers, Searcher inRaws, MultiValued inRun, JSONObject inEmp, Date inNow)
	{
		Object extid = inEmp.get("id");
		if (extid == null)
		{
			throw new IllegalArgumentException("record without id");
		}
		String id = "buk-" + extid;
		MultiValued worker = (MultiValued) inWorkers.searchById(id);
		boolean isnew = worker == null;
		if (isnew)
		{
			worker = (MultiValued) inWorkers.createNewData();
			worker.setId(id);
			worker.setValue("datecreated", inNow);
		}
		String first = str(inEmp.get("first_name"));
		String last = str(inEmp.get("surname"));
		String name = (first + " " + last).trim();
		JSONObject job = inEmp.get("current_job") instanceof JSONObject ? (JSONObject) inEmp.get("current_job") : new JSONObject();

		worker.setValue("fstenant", "fulservice");
		worker.setValue("name", name.isEmpty() ? id : name);
		worker.setValue("fsbukemployeeid", String.valueOf(extid));
		worker.setValue("fsestadotrabajador", mapStatus(str(inEmp.get("status"))));
		worker.setValue("fsactivesince", parseDate(job.get("start_date")));
		worker.setValue("fsactiveuntil", parseDate(job.get("end_date")));
		worker.setValue("fssourceasof", inNow);
		worker.setValue("datemodified", inNow);
		inWorkers.saveData(worker);

		// Redacted copy only: whitelist, never the raw payload
		JSONObject safe = new JSONObject();
		safe.put("id", extid);
		safe.put("name", name);
		safe.put("status", inEmp.get("status"));
		safe.put("role", job.get("role") instanceof JSONObject ? ((JSONObject) job.get("role")).get("name") : job.get("role_name"));
		safe.put("cost_center", job.get("cost_center"));
		safe.put("ficha", inEmp.get("code_sheet"));
		MultiValued raw = (MultiValued) inRaws.createNewData();
		raw.setValue("fstenant", "fulservice");
		raw.setValue("fssistemafuente", "buk");
		raw.setValue("fsejecucionfuente", inRun.getId());
		raw.setValue("fsrecordtype", "employee");
		raw.setValue("fsexternalid", String.valueOf(extid));
		raw.setValue("fspayloadjson", safe.toJSONString());
		raw.setValue("fsredacted", true);
		raw.setValue("fsingestedat", inNow);
		raw.setValue("datecreated", inNow);
		inRaws.saveData(raw);
		worker.setValue("fsregistrocrudo", raw.getId());
		inWorkers.saveData(worker);
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
