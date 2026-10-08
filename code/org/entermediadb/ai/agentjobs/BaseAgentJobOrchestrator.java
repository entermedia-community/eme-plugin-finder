package org.entermediadb.ai.agentjobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;

public abstract class BaseAgentJobOrchestrator implements AgentJobOrchestrator, CatalogEnabled
{
	private static final Log log = LogFactory.getLog(BaseAgentJobOrchestrator.class);

	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected String fieldCatalogId;

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public AgentJobManager getAgentJobManager()
	{
		return (AgentJobManager) getModuleManager().getBean(getCatalogId(), "agentJobManager", true);
	}

	public void startJob(AgentJobRunnable inAgentJob)
	{
		try
		{
			for (AgentJobStep step : inAgentJob.getAgentJob().getAllSteps())
			{
				runStep(inAgentJob, step);
				finishedStep(inAgentJob, step);
			}
			inAgentJob.setCompleted(true);
		}
		catch (Exception e)
		{
			log.error("Errors running agentjob " + inAgentJob.getAgentJob(), e);
		}
		finally
		{
			if (inAgentJob.hasComplete())
			{
				finishedAllSteps(inAgentJob);
			}
			finishedRun(inAgentJob);
		}
	}

	public void finishedStep(AgentJobRunnable inAgentJob, AgentJobStep inStep)
	{
		//Do nothing
	}

	public void finishedAllSteps(AgentJobRunnable inAgentJob)
	{
		getAgentJobManager().finishedAllSteps(inAgentJob);
	}

	public void finishedRun(AgentJobRunnable inAgentJob)
	{
		getAgentJobManager().finishedRun(inAgentJob);
	}

	public void runStep(AgentJobRunnable inAgentJob, AgentJobStep inAgentJobStep)
	{
		MultiValued inStep = inAgentJobStep.getAgentJobStepData();
		try
		{
			processStep(inAgentJob, inStep);
		}
		catch (Exception ex)
		{
			log.error("Error running step " + inStep.getId(), ex);
			inStep.setValue("status", "error");
			inStep.setValue("errordetails", ex.getMessage());
			inAgentJob.getAgentJob().setValue("status", "error");
			getMediaArchive().saveData("agentjob", inAgentJob.getAgentJob());

			if(ex instanceof RuntimeException)
			{
				throw (RuntimeException) ex;
			}
			throw new RuntimeException(ex);
		}
		finally
		{
			getMediaArchive().saveData("agentjobstep", inStep);
		}
	}

	protected abstract void processStep(AgentJobRunnable inAgentJob, MultiValued inStep) throws Exception;

	/**
	 * A skill may leave the step in a non-terminal state (e.g. "waitinginput" while a
	 * long-running external job is still processing or needs a question answered).
	 * Only stamp "complete" if the skill didn't already decide the outcome itself.
	 */
	protected void completeIfRunning(MultiValued inStep)
	{
		if ("running".equals(inStep.get("status")))
		{
			inStep.setValue("status", "complete");
			getMediaArchive().saveData("agentjobstep", inStep);
		}
	}
}
