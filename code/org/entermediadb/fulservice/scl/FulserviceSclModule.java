package org.entermediadb.fulservice.scl;

import java.util.Calendar;
import java.util.Date;

import org.entermediadb.ai.agentjobs.AgentJobManager;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.DateStorageUtil;

/** Endpoints for the agent_job_monitor SCL panel: status, history and "import now". */
public class FulserviceSclModule extends BaseMediaModule
{
	protected AgentJobManager getJobManager(WebPageRequest inReq)
	{
		return (AgentJobManager) getModuleManager().getBean(inReq.findValue("catalogid"), "agentJobManager", true);
	}

	/** Makes sure the repeating scl_sync agentjob exists (seeded from the scl_sync scenario). */
	protected Data ensureJob(WebPageRequest inReq, MediaArchive inArchive)
	{
		Data job = inArchive.query("agentjob").exact("id", SclImportManager.JOB_ID).searchOne();
		if (job == null && inArchive.getCachedData("automationscenario", SclImportManager.JOB_ID) != null)
		{
			job = getJobManager(inReq).importRecurringScenario(SclImportManager.JOB_ID, inReq.getUserName());
		}
		return job;
	}

	protected Data lastRun(MediaArchive inArchive, String inStatus)
	{
		var q = inArchive.query("fsejecucionfuente").exact("fssistemafuente", "scl");
		if (inStatus != null)
		{
			q.exact("fsestadoejecucion", inStatus);
		}
		return q.sort("fsstartedatDown").searchOne();
	}

	protected JSONObject toJson(Data inRun)
	{
		if (inRun == null)
		{
			return null;
		}
		JSONObject o = new JSONObject();
		o.put("id", inRun.getId());
		o.put("type", inRun.get("fstipoejecucion"));
		o.put("status", inRun.get("fsestadoejecucion"));
		o.put("startedAt", iso(inRun.getValue("fsstartedat")));
		o.put("finishedAt", iso(inRun.getValue("fsfinishedat")));
		o.put("recordsRead", inRun.get("fsrecordsread"));
		o.put("recordsWritten", inRun.get("fsrecordswritten"));
		o.put("recordsRejected", inRun.get("fsrecordsrejected"));
		o.put("errorCode", inRun.get("fserrorcode"));
		o.put("errorDetail", inRun.get("fserrordetail"));
		return o;
	}

	protected String iso(Object inDate)
	{
		Date d = DateStorageUtil.getStorageUtil().parseFromObject(inDate);
		return d == null ? null : new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(d);
	}

	public void status(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Data job = ensureJob(inReq, archive);
		JSONObject out = new JSONObject();
		out.put("configured", new SclClient().isConfigured());
		out.put("jobId", SclImportManager.JOB_ID);
		out.put("jobExists", job != null);
		out.put("running", SclImportManager.isRunning());
		Data last = lastRun(archive, null);
		out.put("lastRun", toJson(last));
		out.put("lastSuccess", toJson(lastRun(archive, "success")));
		if (job != null)
		{
			out.put("jobStatus", job.get("status"));
			out.put("repeatPeriod", job.get("repeatperiod"));
			out.put("repeatTime", job.get("repeattime"));
			out.put("nextRun", nextRun(job, last));
		}
		inReq.putPageValue("json", out.toJSONString());
	}

	/** Approximation of AgentJobManager.isRepeatDue for display: minute/hour count from last run, others from repeattime. */
	protected String nextRun(Data inJob, Data inLast)
	{
		String period = inJob.get("repeatperiod");
		if (period == null)
		{
			return null;
		}
		Calendar c = Calendar.getInstance();
		Date lastStart = inLast == null ? null : DateStorageUtil.getStorageUtil().parseFromObject(inLast.getValue("fsstartedat"));
		switch (period)
		{
			case "minute":
				c.setTime(lastStart == null ? new Date() : lastStart);
				c.add(Calendar.MINUTE, 1);
				return iso(c.getTime());
			case "hour":
				c.setTime(lastStart == null ? new Date() : lastStart);
				c.add(Calendar.HOUR_OF_DAY, 1);
				return iso(c.getTime());
			default:
				String time = inJob.get("repeattime");
				int h = 0, m = 0;
				if (time != null && time.matches("\\d{1,2}:\\d{2}"))
				{
					h = Integer.parseInt(time.split(":")[0]);
					m = Integer.parseInt(time.split(":")[1]);
				}
				c.set(Calendar.HOUR_OF_DAY, h);
				c.set(Calendar.MINUTE, m);
				c.set(Calendar.SECOND, 0);
				if (c.getTime().before(new Date()))
				{
					c.add(Calendar.DAY_OF_YEAR, "weekly".equals(period) ? 7 : "monthly".equals(period) ? 30 : 1);
				}
				return iso(c.getTime());
		}
	}

	public void runs(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		HitTracker hits = archive.query("fsejecucionfuente").exact("fssistemafuente", "scl").sort("fsstartedatDown").search();
		hits.setHitsPerPage(25);
		String page = inReq.getRequestParameter("page");
		if (page != null && page.matches("\\d+"))
		{
			hits.setPage(Integer.parseInt(page));
		}
		JSONArray arr = new JSONArray();
		for (Object o : hits.getPageOfHits())
		{
			arr.add(toJson((Data) o));
		}
		JSONObject out = new JSONObject();
		out.put("total", hits.size());
		out.put("results", arr);
		inReq.putPageValue("json", out.toJSONString());
	}

	public void importNow(WebPageRequest inReq)
	{
		JSONObject out = new JSONObject();
		if (inReq.getUser() == null)
		{
			inReq.getResponse().setStatus(401);
			out.put("ok", false);
			out.put("error", "Login required");
		}
		else if (!new SclClient().isConfigured())
		{
			inReq.getResponse().setStatus(412);
			out.put("ok", false);
			out.put("error", "SCL credentials are not configured on the server");
		}
		else if (SclImportManager.isRunning())
		{
			inReq.getResponse().setStatus(409);
			out.put("ok", false);
			out.put("error", "An import is already running");
		}
		else
		{
			MediaArchive archive = getMediaArchive(inReq);
			if (ensureJob(inReq, archive) == null)
			{
				inReq.getResponse().setStatus(500);
				out.put("ok", false);
				out.put("error", "Scenario scl_sync is not installed");
			}
			else
			{
				getJobManager(inReq).rerunJob(SclImportManager.JOB_ID);
				out.put("ok", true);
				out.put("jobId", SclImportManager.JOB_ID);
			}
		}
		inReq.putPageValue("json", out.toJSONString());
	}
}
