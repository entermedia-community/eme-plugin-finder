package org.entermediadb.fulservice.scl;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.fulservice.buk.BukSyncSkill;
import org.openedit.Data;

/** Agent skill behind the repeating "scl_sync" job: captures and parses the SCL contractor page. */
public class SclSyncSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		final MediaArchive archive = getMediaArchive();
		final String jobid = inContext.getCurrentAgentJob() == null ? SclImportManager.JOB_ID : inContext.getCurrentAgentJob().getId();
		final String stepid = inContext.getCurrentAgentJobStep() == null ? "" : inContext.getCurrentAgentJobStep().getId();

		SclImportManager manager = new SclImportManager(archive);
		Data run = manager.runImport("full", jobid, null, (type, message) -> BukSyncSkill.appendLog(archive, jobid, stepid, type, message));

		BasicLlmResponse response = new BasicLlmResponse();
		if (run == null)
		{
			response.setMessage("SCL import skipped: another import is running.");
		}
		else
		{
			response.setMessage("SCL import " + run.get("fsestadoejecucion") + ": " + run.get("fsrecordsread") + " rows read."
					+ (run.get("fserrorcode") == null ? "" : " Error: " + run.get("fserrorcode")));
		}
		inContext.setLastResponse(response);
		super.process(inContext);
	}
}
