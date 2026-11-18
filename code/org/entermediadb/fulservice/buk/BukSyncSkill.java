package org.entermediadb.fulservice.buk;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;

/** Agent skill behind the repeating "buk_sync" job: imports BUK employees and logs progress to the job run. */
public class BukSyncSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		final MediaArchive archive = getMediaArchive();
		final String jobid = inContext.getCurrentAgentJob() == null ? BukImportManager.JOB_ID : inContext.getCurrentAgentJob().getId();
		final String stepid = inContext.getCurrentAgentJobStep() == null ? "" : inContext.getCurrentAgentJobStep().getId();

		BukImportManager manager = new BukImportManager(archive);
		Data run = manager.runImport("full", jobid, null, (type, message) -> appendLog(archive, jobid, stepid, type, message));

		BasicLlmResponse response = new BasicLlmResponse();
		if (run == null)
		{
			response.setMessage("BUK import skipped: another import is running.");
		}
		else
		{
			response.setMessage("BUK import " + run.get("fsestadoejecucion") + ": " + run.get("fsrecordsread") + " read, "
					+ run.get("fsrecordswritten") + " saved, " + run.get("fsrecordsrejected") + " rejected.");
		}
		inContext.setLastResponse(response);
		super.process(inContext);
	}

	/** Same format AgentJobManager.createAgentLog writes (step, type, log, timestamp), onto the latest run. */
	public static void appendLog(MediaArchive inArchive, String inJobId, String inStepId, String inType, String inMessage)
	{
		MultiValued run = (MultiValued) inArchive.query("agentjobrun").exact("agentjob", inJobId).sort("startdateDown").searchOne();
		if (run == null)
		{
			return;
		}
		JSONObject entry = new JSONObject();
		entry.put("step", inStepId);
		entry.put("type", inType);
		entry.put("log", inMessage);
		entry.put("timestamp", String.valueOf(System.currentTimeMillis()));
		run.addValue("agentlogs", entry.toJSONString());
		inArchive.saveData("agentjobrun", run);
	}
}
