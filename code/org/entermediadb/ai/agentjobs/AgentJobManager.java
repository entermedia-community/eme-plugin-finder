package org.entermediadb.ai.agentjobs;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.Skill;
import org.entermediadb.ai.SkillStatusListener;
import org.entermediadb.ai.automation.AutomationManager;
import org.entermediadb.ai.agentjobs.AgentJobStep;
import org.entermediadb.ai.llm.BaseAgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.mcp.client.OpenCodeClient;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.websocket.chat.ChatServer;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.ExecutorManager;
import org.openedit.util.OutputFiller;

public class AgentJobManager implements SkillStatusListener, CatalogEnabled
{
	private static final Log log = LogFactory.getLog(AgentJobManager.class);

	public static final String DEFAULT_ORCHESTRATOR = "javaskillOrchestrator";

	/** automationstep fields that are not copied to the agentjobstep. runafter and aiskill are mapped instead */
	protected static final Set<String> TEMPLATE_ONLY_FIELDS = Set.of("id", "enabled", "aiskill", "runafter", "ordering", "offsetx", "offsety");

	
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected Map currentJobsRunning = new ConcurrentHashMap();
	protected String fieldCatalogId;
	protected int fieldTotalPending;
	protected OpenCodeClient fieldOpenCodeClient;

	public int getMaxProcessors()
	{
		if (fieldMaxProcessors == -1)
		{
			String max = getMediaArchive().getCatalogSettingValue("conversion_max_processors");
			if (max != null)
			{
				fieldMaxProcessors = Integer.parseInt(max);
			}
			else
			{
				fieldMaxProcessors = getThreads().getAvailableProcessors() - 1;
			}
			if (fieldMaxProcessors < 1)
			{
				fieldMaxProcessors = 1;
			}
		}
		return fieldMaxProcessors;
	}

	public void setMaxProcessors(int inMaxProcessors)
	{
		fieldMaxProcessors = inMaxProcessors;
	}

	protected int fieldMaxProcessors = -1;

	public int getTotalPending()
	{
		return fieldTotalPending;
	}

	public void setTotalPending(int inTotalPending)
	{
		fieldTotalPending = inTotalPending;
	}

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
	public OpenCodeClient getOpenCodeClient()
	{
		if (fieldOpenCodeClient == null)
		{
			fieldOpenCodeClient = (OpenCodeClient) getModuleManager().getBean(getCatalogId(), "openCodeClient");
		}
		return fieldOpenCodeClient;
	}

	public void setOpenCodeClient(OpenCodeClient inOpenCodeClient)
	{
		fieldOpenCodeClient = inOpenCodeClient;
	}
	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public synchronized void checkQueue()
	{
		if (!hasAvailableProcessor())
		{
			log.info("No available processors");
			return;
		}

		// Lock searching for tasks
		try
		{
			HitTracker newjobs = getNewjobs();
			setTotalPending(newjobs.size());
			if (newjobs.size() > 0)
			{
				log.info("processing " + newjobs.size() + " AgentJob with statuses: new submitted retry missinginput");
			}
			else
			{
				return;
			}
			for (Iterator iterator = newjobs.iterator(); iterator.hasNext();)
			{
				// lock and create
				if (currentJobsRunning.size() >= availableProcessors())
				{
					log.info("reached a full queue. Waiting till some complete before adding more jobs");
					break;
				}
				Data hit = (Data) iterator.next();

				AgentJob job = (AgentJob)getMediaArchive().getCachedData("agentjob", hit.getId());
				if( job.getValue("status") != null && job.getValue("status").equals("running"))
				{
					log.info("Skipping job " + job + " as it is already running");
					continue;
				}
				AgentJobOrchestrator orchestrator;
				try
				{
					orchestrator = getOrchestrator(job);
				}
				catch (Exception ex)
				{
					log.error("Could not start agentjob " + job.getId(), ex);
					job.setValue("status", "error");
					job.setValue("errordetails", ex.getMessage());
					getMediaArchive().saveData("agentjob", job);
					continue;
				}

				job.setValue("status","running");
				getMediaArchive().saveData("agentjob", job);

				//Run it now
				AgentJobRunnable torun = new AgentJobRunnable();
				AgentContext context = createAgentContext();
				context.setCatalogId(getCatalogId());
				context.setModuleManager(getModuleManager());
				context.put("agentjob", job);
				context.setCurrentAgentJob(job);
				torun.setContext(context);
				torun.setAgentJob(job);
				torun.setAgentJobOrchestrator(orchestrator);
				torun.setAgentJobRun(createAgentJobRun(job));
				addAgentJob(torun);
			}
		}
		catch (Throwable ex)
		{
			log.error("Could not process queue ", ex);
		}
	}

	public HitTracker getNewjobs()
	{
		Searcher jobsearcher = getMediaArchive().getSearcher("agentjob");

		QueryBuilder query = getMediaArchive().localQuery("agentjob");
		query.orgroup("status", "new"); 
		query.sort("submitteddateDown");

		if (hasRunningAgentJob()) //Skip these just in case
		{
			query.notgroup("id", getRunningIds());
		}
		HitTracker newjobs = jobsearcher.search(query.getQuery());
		newjobs.enableBulkOperations();
		newjobs.setHitsPerPage(500); // Just enought to fill up the queue
		return newjobs;
	}

	public Map getCurrentJobsRunning()
	{
		return currentJobsRunning;
	}

	private boolean hasRunningAgentJob()
	{
		return currentJobsRunning.isEmpty() == false;
	}

	public Set getRunningIds()
	{
		return currentJobsRunning.keySet();
	}

	private boolean hasAvailableProcessor()
	{
		return availableProcessors() > 0;
	}

	private int availableProcessors()
	{
		int total = getMaxProcessors();
		total = total - currentJobsRunning.size();
		return total;
	}

	public int runningProcesses()
	{
		return currentJobsRunning.size();
	}

	private void addAgentJob(AgentJobRunnable inAgentJob)
	{
		if (currentJobsRunning.size() >= availableProcessors())
		{
			//Should we save?
			return;
		}

		currentJobsRunning.put(inAgentJob.getId(), inAgentJob);
		getThreads().execute("importing", inAgentJob);
	}

	/**
	 * Picks the orchestrator bean named by the agentjob's "orchestrator" field (see the chosenorchestrator list).
	 * Jobs without one run their steps as Java skills.
	 */
	public AgentJobOrchestrator getOrchestrator(AgentJob inJob)
	{
		String orchestratorid = inJob.get("orchestrator");
		if( orchestratorid == null || orchestratorid.trim().isEmpty())
		{
			orchestratorid = DEFAULT_ORCHESTRATOR;
		}
		Object bean = getModuleManager().getBean(getCatalogId(), orchestratorid, true);		
		return (AgentJobOrchestrator) bean;
	}

	/** Called by the orchestrator once every step ran */
	public void finishedAllSteps(AgentJobRunnable inAgentJob)
	{
		try
		{
			currentJobsRunning.remove(inAgentJob.getId());
			log.info("RELEASED " + inAgentJob.getId());
			//Save job
			//Do we have this last response from the steps?
			//String lastresponse = inAgentJob.getAgentJob().findLastResponse();
			//inAgentJob.getAgentJob().setValue("lastresponse", lastresponse);
			
			inAgentJob.getAgentJob().setValue("status", "complete");
			inAgentJob.getAgentJob().setValue("enddate", new Date());
			getMediaArchive().saveData("agentjob", inAgentJob.getAgentJob());

			checkQueue();
		}
		catch (Exception ex)
		{
			log.error("Problem finishing AgentJob ", ex);
		}
	}



	/** Called by the orchestrator when the job exits, even after an error */
	public void finishedRun(AgentJobRunnable inAgentJob)
	{
		currentJobsRunning.remove(inAgentJob.getId()); //Also released when a step threw an error
		Data run = inAgentJob.getAgentJobRun();
		if( run == null)
		{
			return;
		}
		try
		{
			AgentJob job = inAgentJob.getAgentJob();
			StringBuffer markdown = new StringBuffer();
			for (AgentJobStep step : job.getAllSteps())
			{
				Data saved = getMediaArchive().getCachedData("agentjobstep", step.getId());
				String content = saved == null ? null : saved.get("markdowncontent");
				if( content != null && !content.isEmpty())
				{
					if( markdown.length() > 0)
					{
						markdown.append("\n\n---\n\n");
					}
					markdown.append(content);
				}
			}
			run.setValue("markdowncontent", trimToFit(markdown.toString()));
			run.setValue("enddate", new Date());
			run.setValue("status", inAgentJob.hasComplete() ? "complete" : "error");
			getMediaArchive().saveData("agentjobrun", run);
		}
		catch (Exception ex)
		{
			log.error("Could not save agentjobrun " + run.getId(), ex);
		}
	}

	protected Data createAgentJobRun(AgentJob inJob)
	{
		Data run = getMediaArchive().getSearcher("agentjobrun").createNewData();
		run.setValue("agentjob", inJob.getId());
		run.setValue("startdate", new Date());
		run.setValue("status", "running");
		getMediaArchive().saveData("agentjobrun", run);
		return run;
	}

	/** Lucene rejects terms over 32766 bytes, so cut the end off long logs */
	protected String trimToFit(String inText)
	{
		if( inText == null || inText.getBytes(StandardCharsets.UTF_8).length <= 30000)
		{
			return inText;
		}
		return new OutputFiller().splitUtf8(inText, 30000).get(0);
	}

	/**
	 * Queues any agentjob whose repeatperiod (daily, weekly, monthly) and repeattime (HH:mm) say it is due.
	 * Due jobs are set back to "new" so checkQueue picks them up.
	 * @return how many jobs were queued
	 */
	public int checkRepeatingJobs()
	{
		HitTracker jobs = getMediaArchive().query("agentjob").exists("repeatperiod").search();
		jobs.enableBulkOperations();
		int count = 0;
		for (Iterator iterator = jobs.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			try
			{
				AgentJob job = (AgentJob) getMediaArchive().getCachedData("agentjob", hit.getId());
				String status = job.get("status");
				//Only repeat jobs that are finished. Skip running, queued or ones waiting on a person
				if( status != null && !"complete".equals(status) && !"error".equals(status))
				{
					continue;
				}
				if( currentJobsRunning.containsKey(job.getId()) || !isRepeatDue(job, new Date()))
				{
					continue;
				}
				resetForRepeat(job);
				count++;
			}
			catch (Exception ex)
			{
				log.error("Could not check repeating agentjob " + hit.getId(), ex);
			}
		}
		if( count > 0)
		{
			checkQueue();
		}
		return count;
	}

	public boolean isRepeatDue(AgentJob inJob, Date inNow)
	{
		String period = inJob.get("repeatperiod");
		LocalTime time = parseRepeatTime(inJob.get("repeattime"));
		if( period == null || time == null)
		{
			return false;
		}
		ZoneId zone = ZoneId.systemDefault();
		LocalDateTime now = LocalDateTime.ofInstant(inNow.toInstant(), zone);

		Data lastrun = getMediaArchive().query("agentjobrun").exact("agentjob", inJob.getId()).sort("startdateDown").searchOne();
		Date laststart = lastrun == null ? null : ((MultiValued) lastrun).getDate("startdate");
		if( laststart == null)
		{
			//Never run before, start at the next repeattime
			return !now.isBefore(LocalDateTime.of(now.toLocalDate(), time));
		}
		//Snap to repeattime on the day of the last run so late runs do not drift
		LocalDate lastday = LocalDateTime.ofInstant(laststart.toInstant(), zone).toLocalDate();
		LocalDateTime next;
		switch (period)
		{
			case "daily":
				next = LocalDateTime.of(lastday.plusDays(1), time);
				break;
			case "weekly":
				next = LocalDateTime.of(lastday.plusWeeks(1), time);
				break;
			case "monthly":
				next = LocalDateTime.of(lastday.plusMonths(1), time);
				break;
			default:
				log.error("Unknown repeatperiod " + period + " on agentjob " + inJob.getId());
				return false;
		}
		return !now.isBefore(next);
	}

	protected LocalTime parseRepeatTime(String inTime)
	{
		if( inTime == null || inTime.trim().isEmpty())
		{
			return null;
		}
		String[] parts = inTime.trim().split(":");
		try
		{
			int hour = Integer.parseInt(parts[0]);
			int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
			return LocalTime.of(hour, minute);
		}
		catch (Exception ex)
		{
			log.error("Invalid repeattime " + inTime + " expected HH:mm like 03:40");
			return null;
		}
	}

	protected void resetForRepeat(AgentJob inJob)
	{
		inJob.setSteps(null); //Reload from database
		for (AgentJobStep step : inJob.getAllSteps())
		{
			step.setValue("status", "new");
			step.setValue("errordetails", null);
			getOpenCodeClient().clearStatus(step.getId()); //Start a new opencode session
			getMediaArchive().saveData("agentjobstep", step.getAgentJobStepData());
		}
		inJob.setValue("status", "new");
		inJob.setValue("errordetails", null);
		inJob.setValue("submitteddate", new Date());
		inJob.setValue("startdate", new Date());
		inJob.setValue("enddate", null);
		getMediaArchive().saveData("agentjob", inJob);
		log.info("Queued repeating agentjob " + inJob.getId());
	}

	public ExecutorManager getThreads()
	{
		ExecutorManager queue = (ExecutorManager) getModuleManager().getBean(getMediaArchive().getCatalogId(), "executorManager");
		return queue;
	}

	/**
	 * Creates a running agentjob with a single agentjobstep for the given skill from a chat message.
	 * The job is saved as "running" so checkQueue does not pick it up; the caller runs the step itself.
	 */
	public AgentJob createAgentJobFromMessage(String inUserId, String inUserMessage, String inscenarioId, String inAiSkillId)
	{

		AgentJob job = (AgentJob) getMediaArchive().getSearcher("agentjob").createNewData();
		job.setValue("owner", inUserId);
		job.setValue("submitteddate", new Date());
		job.setValue("startdate", new Date());
		job.setValue("status", "new");
		job.setValue("userrequest", inUserMessage);
		job.setValue("name", inUserMessage);
		getMediaArchive().saveData("agentjob", job);

		MultiValued step = (MultiValued) getMediaArchive().getSearcher("agentjobstep").createNewData();
		step.setValue("agentjob", job.getId());
		step.setValue("aiskillid", inAiSkillId);
		step.setValue("automationscenario", inscenarioId);
		step.setValue("status", "new");
		step.setValue("userrequest", inUserMessage);
		getMediaArchive().saveData("agentjobstep", step);

		job.setSteps(null);
		return job;
	}

	public AutomationManager getAutomationManager()
	{
		return (AutomationManager) getModuleManager().getBean(getCatalogId(), "automationManager", true);
	}

	/**
	 * Saves a new agentjob with a copy of each enabled automationstep of the scenario as an agentjobstep.
	 * Each copy keeps the automationstep id and its "runafter" points at the copied parent step.
	 */
	public AgentJob importScenario(String inScenarioId, AgentContext inContext)
	{
		AgentJob job = (AgentJob) getMediaArchive().getSearcher("agentjob").createNewData();
		job.setValue("automationscenario", inScenarioId);
		MultiValued scenariodata = (MultiValued) getMediaArchive().getCachedData("automationscenario", inScenarioId);
		job.setValue("name", scenariodata == null ? inScenarioId : scenariodata.getName());
		if (inContext != null && inContext.getUserProfile() != null)
		{
			job.setValue("owner", inContext.getUserProfile().getUserId());
		}
		Object userrequest = inContext == null ? null : inContext.getContextValue("userrequest");
		if (userrequest instanceof String)
		{
			job.setValue("userrequest", userrequest);
		}
		job.setValue("submitteddate", new Date()); //No status so checkQueue leaves it alone until it runs
		getMediaArchive().saveData("agentjob", job);

		Collection<MultiValued> templates = getMediaArchive().query("automationstep").exact("automationscenario", inScenarioId).exact("enabled", true).sort("orderingUp").search();
		Map<String, MultiValued> copies = new HashMap<String, MultiValued>();
		Collection<MultiValued> tosave = new ArrayList<MultiValued>();
		for (MultiValued template : templates)
		{
			MultiValued step = (MultiValued) getMediaArchive().getSearcher("agentjobstep").createNewData();
			for (Object key : template.getProperties().keySet())
			{
				if (!TEMPLATE_ONLY_FIELDS.contains(key))
				{
					step.setValue((String) key, template.getValue((String) key));
				}
			}
			step.setValue("agentjob", job.getId());
			step.setValue("automationstep", template.getId());
			step.setValue("automationscenario", inScenarioId);
			step.setValue("aiskillid", template.get("aiskill"));
			step.setValue("status", "new");
			copies.put(template.getId(), step);
			tosave.add(step);
		}
		getMediaArchive().saveData("agentjobstep", tosave);

		//Now that the copies have ids point runafter at them
		for (MultiValued template : templates)
		{
			String runafter = template.get("runafter");
			MultiValued parent = runafter == null ? null : copies.get(runafter);
			if (parent != null)
			{
				copies.get(template.getId()).setValue("runafter", parent.getId());
			}
		}
		getMediaArchive().saveData("agentjobstep", tosave);

		job.setSteps(null);
		return job;
	}

	public AgentContext createAgentContext()
	{
		String contextbeanname = "baseAgentContext";
		AgentContext childContext = (AgentContext) getMediaArchive().getBean(contextbeanname, false);
		return childContext;
	}
	public AgentContext createAgentContext(AgentJobStep inStep)
	{
		return createAgentContext(null, inStep);
	}

	/** A child context using the step's aiskill "contextbean", or baseAgentContext */
	public AgentContext createAgentContext(AgentContext inParentContext, AgentJobStep inStep)
	{
		String contextbeanname = inStep.getAgentData() == null ? null : inStep.getAgentData().get("contextbean");
		if (contextbeanname == null)
		{
			contextbeanname = "baseAgentContext";
		}
		AgentContext childContext = (AgentContext) getMediaArchive().getBean(contextbeanname, false);
		childContext.setCurrentAutomationStep(inStep);
		if (inParentContext != null)
		{
			childContext.setParentContext(inParentContext);
		}
		return childContext;
	}

	public void runScenario(String inId, ScriptLogger inLogger)
	{
		AgentContext context = createAgentContext();
		context.setScriptLogger(inLogger);
		runScenario(inId, context);
	}

	public void runScenario(String inId, UserProfile inUserProfile, Map inContextMap, String inAutomationStepId, ScriptLogger inLogger)
	{
		AgentContext context = createAgentContext();
		context.putContextValues(inContextMap);
		context.setScriptLogger(inLogger);
		context.setUserProfile(inUserProfile);
		AgentJob job = importScenario(inId, context);

		AgentJobStep step = null;
		if (inAutomationStepId != null)
		{
			step = job.findEnabled(inAutomationStepId);
		}
		else if (!job.getSteps().isEmpty())
		{
			step = job.getSteps().iterator().next();
		}
		if (step == null)
		{
			log.error("No step " + inAutomationStepId + " in scenario " + inId);
			return;
		}
		AgentContext stepcontext = createAgentContext(step);
		stepcontext.putContextValues(inContextMap);
		stepcontext.setScriptLogger(inLogger);
		stepcontext.setUserProfile(inUserProfile);
		runScenario(job, stepcontext);
	}

	public void runScenario(String inId, AgentContext inContext)
	{
		runScenario(importScenario(inId, inContext), inContext);
	}

	public void runScenario(AgentJob inJob, AgentContext inContext)
	{
		if (inContext.getId() == null)
		{
			inContext.setId(getAutomationManager().inCrementId());
		}
		getAutomationManager().addContext(inJob.getScenarioId(), inContext);
		inContext.setCurrentAgentJob(inJob);

		AgentJobStep step = inContext.getCurrentAutomationStep();
		if (step == null)
		{
			log.error("Scenario " + inJob.getScenarioId() + " has no enabled steps");
			return;
		}
		AgentContext currentContext = createAgentContext(inContext, step);
		runProcess(currentContext, step);
	}

	/**
	 * Runs a step by its automationstep id. Use "scenario.stepid" to switch to a new agentjob copied from
	 * that scenario, or just "stepid" to stay in the context's current agentjob.
	 */
	public boolean runProcess(AgentContext inContext, String inFunctionParts)
	{
		log.info("Running scenario: " + inFunctionParts);
		String[] parts = inFunctionParts.split("\\.");
		String stepid = null;
		AgentJob job = inContext.getCurrentAgentJob();
		if (parts.length > 1)
		{
			if (job == null || !parts[0].equals(job.getScenarioId()))
			{
				job = importScenario(parts[0], inContext);
				inContext.setCurrentAgentJob(job);
			}
			stepid = parts[1];
		}
		else
		{
			stepid = parts[0];
		}
		if (job == null)
		{
			log.error("No agentjob set on context for step: " + inFunctionParts);
			return false;
		}

		AgentJobStep step = job.findEnabled(stepid);
		if (step == null)
		{
			log.error("No step found for id: " + stepid + " in agentjob " + job.getId());
			return false;
		}
		AgentContext currentContext = createAgentContext(inContext, step);
		return runProcess(currentContext, step);
	}

	public boolean runProcess(AgentContext inContext, AgentJobStep inStep)
	{
		return runProcess(inContext, inStep, false);
	}

	/**
	 * Runs one step of the context's agentjob and saves the step's status. Nested steps (children,
	 * runskill, exec) run inside this call, and the outermost call finishes the agentjob.
	 */
	public boolean runProcess(AgentContext inContext, AgentJobStep inStep, boolean inSkipStatusStart)
	{
		inContext.setCurrentAutomationStep(inStep);
		inContext.addStatusListener(this);

		AgentJob job = inContext.getCurrentAgentJob();
		boolean ownsjob = job != null && job.getRunDepth() == 0;
		if (job != null)
		{
			job.setRunDepth(job.getRunDepth() + 1);
		}
		if (ownsjob)
		{
			job.setValue("status", "running");
			job.setValue("errordetails", null);
			job.setValue("startdate", new Date());
			job.setValue("enddate", null);
			getMediaArchive().saveData("agentjob", job);
		}
		inStep.setValue("status", "running");
		inStep.setValue("errordetails", null);
		getMediaArchive().saveData("agentjobstep", inStep.getAgentJobStepData());

		try
		{
			return runStep(inContext, inStep, inSkipStatusStart);
		}
		catch (RuntimeException ex)
		{
			inStep.setValue("status", "error");
			inStep.setValue("errordetails", ex.getMessage());
			throw ex;
		}
		finally
		{
			finishStep(job, inStep, inContext);
			if (job != null)
			{
				job.setRunDepth(job.getRunDepth() - 1);
			}
			if (ownsjob)
			{
				finishJob(job);
			}
		}
	}

	protected boolean runStep(AgentContext inContext, AgentJobStep inStep, boolean inSkipStatusStart)
	{
		Skill agent = inStep.getAgent();
		if (agent == null)
		{
			log.error("No agent found for step " + inStep.getEnabledId());
			inStep.setValue("status", "error");
			inStep.setValue("errordetails", "No agent found for step " + inStep.getEnabledId());
			return false;
		}
		if (!inSkipStatusStart)
		{
			agent.processStarting(inContext);
		}
		log.info("Running agentjob: " + inContext.getCurrentAgentJob() + "  step: " + inStep.getEnabledId());
		agent.process(inContext);

		LlmResponse response = inContext.getLastResponse();
		if (response == null)
		{
			//Skills that do not call an LLM leave no response, so the step still completes
			log.error("No response from " + inContext.getCurrentAgentJob() + " running " + inStep.getEnabledId());
			return false;
		}

		String state = response.getOperationState();
		if ("error".equals(state))
		{
			log.error("Error from " + inContext.getCurrentAgentJob() + " running " + inStep.getEnabledId() + ": " + response.getMessage());
			inStep.setValue("status", "error");
			inStep.setValue("errordetails", response.getMessage());
			return false;
		}
		/// error, cancel, continue, runskill
		if ("cancel".equals(state))
		{
			// Just return without broadcasting or saving anything. This is for when the function is called but
			// we determine we dont need to do anything.
			return false;
		}
		else if ("runskill".equals(state))
		{
			runProcess(inContext, response.getExecAutomationStep());
			return false;
		}
		else if ("needuserinput".equals(state))
		{
			// fire complete should have sent it back to the user
			inStep.setValue("status", "question");
			return false;
		}
		else
		{
			log.info("No status from " + inContext.getCurrentAgentJob() + " running " + inStep.getEnabledId());
		}
		return true;
	}

	protected void finishStep(AgentJob inJob, AgentJobStep inStep, AgentContext inContext)
	{
		try
		{
			if ("running".equals(inStep.get("status")))
			{
				inStep.setValue("status", "complete");
			}
			LlmResponse response = inContext.getLastResponse();
			if (response != null && response.getMessage() != null)
			{
				inStep.setValue("lastresponse", trimToFit(response.getMessage()));
			}
			getMediaArchive().saveData("agentjobstep", inStep.getAgentJobStepData());

			//The job keeps the first error or question
			String status = inStep.get("status");
			if (inJob != null && !"complete".equals(status) && "running".equals(inJob.get("status")))
			{
				inJob.setValue("status", status);
				inJob.setValue("errordetails", inStep.get("errordetails"));
			}
		}
		catch (Exception ex)
		{
			log.error("Could not save agentjobstep " + inStep.getId(), ex);
		}
	}

	protected void finishJob(AgentJob inJob)
	{
		try
		{
			if ("running".equals(inJob.get("status")))
			{
				inJob.setValue("status", "complete");
			}
			inJob.setValue("enddate", new Date());
			getMediaArchive().saveData("agentjob", inJob);
		}
		catch (Exception ex)
		{
			log.error("Could not save agentjob " + inJob.getId(), ex);
		}
	}

	public void handleStatusStarting(AgentContext inContext, AgentJobStep inAutomationStep)
	{
		if (!(inContext instanceof ChatMessageContext))
		{
			return;
		}

		ChatMessageContext chatMessageContext = (ChatMessageContext) inContext;

		boolean skiploader = Boolean.parseBoolean((String) chatMessageContext.getContextValue("skiploader"));

		if (skiploader)
		{
			return;
		}

		MultiValued function = inAutomationStep.getAgentJobStepData();

		JsonUtil jsonUtil = (JsonUtil) getMediaArchive().getBean("jsonUtil");

		String processingmessage = null;
		if (function != null)
		{
			processingmessage = function.get("processingmessage");
		}
		if (processingmessage == null)
		{
			processingmessage = "Analyzing";
		}

		String processingtype = (String) inContext.getContextValue("processingtype");
		if (processingtype != null)
		{
			processingmessage += " " + processingtype;
		}
		String loader = jsonUtil.escape("<i class='fas fa-spinner fa-spin'></i> ");
		processingmessage = loader + processingmessage + "...";
		processingmessage = "<span class='processing-message'>" + processingmessage + "</span>";

		MultiValued agentmessage = chatMessageContext.getAgentMessage();

		String message = inContext.getMessagePrefix() + processingmessage;
		agentmessage.setValue("message", message);
		agentmessage.setValue("messagetype", "status");
		agentmessage.setValue("functionname", inAutomationStep.getEnabledId());
		getMediaArchive().saveData("chatterbox", agentmessage);
		ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");
		server.broadcastMessage(getMediaArchive().getCatalogId(), agentmessage);
	}

	public void handleStatusComplete(AgentContext inContext, AgentJobStep inAutomationStep)
	{
		//Only chat contexts have an agentmessage to send back
		MultiValued agentmessage = (MultiValued) inContext.getContextValue("agentmessage");
		if (agentmessage == null)
		{
			return;
		}
		LlmResponse response = inContext.getLastResponse();

		try
		{
			String updatedMessage = null;
			String messagePrefix = inContext.getMessagePrefix();

			if (response != null && response.getMessage() != null)
			{
				if (messagePrefix != null)
				{
					updatedMessage = messagePrefix + response.getMessage();
				}
				else
				{
					updatedMessage = response.getMessage();
				}
			}
			if (updatedMessage != null)
			{
				agentmessage.setValue("message", updatedMessage); // Final message
			}

			String messageplain = agentmessage.get("messageplain");
			if (response != null)
			{
				String newmessageplain = response.getMessagePlain();

				if (newmessageplain != null)
				{
					if (messageplain == null)
					{
						messageplain = newmessageplain;
					}
					else
					{
						messageplain += " \n " + newmessageplain;
					}
					agentmessage.setValue("messageplain", messageplain);
				}
			}
			String nextFunctionName = null;
			if (response == null)
			{
				log.error("Skipping null responses");
			}
			else
			{
				nextFunctionName = response.getNextAutomationStep();
			}

			if (nextFunctionName == null)
			{
				AgentJobStep nextEnabled = inAutomationStep.getNextAutomationStep();
				if (nextEnabled != null)
				{
					nextFunctionName = nextEnabled.getEnabledId();
				}
			}

			//Make functioname be the last functio
			//and next be the next
			agentmessage.setValue("functionname", inAutomationStep.getEnabledId());
			agentmessage.setValue("nextfunctionname", nextFunctionName);
			agentmessage.setValue("chatmessagestatus", "completed");

			agentmessage.setValue("agentcontextvalues", inContext.toJSONString());

			getMediaArchive().saveData("chatterbox", agentmessage);

			Map<String, String> functionMessageUpdate = new HashMap<>();
			functionMessageUpdate.put("messagetype", "airesponse");
			functionMessageUpdate.put("catalogid", getMediaArchive().getCatalogId());
			functionMessageUpdate.put("user", "agent");
			functionMessageUpdate.put("channel", agentmessage.get("channel"));
			functionMessageUpdate.put("messageid", agentmessage.getId());
			if (messageplain == null)
			{
				messageplain = "New message";
			}
			functionMessageUpdate.put("message", updatedMessage);
			functionMessageUpdate.put("agentcontextvalues", agentmessage.get("agentcontextvalues"));
			functionMessageUpdate.put("messageplain", messageplain);
			functionMessageUpdate.put("nextfunctionname", nextFunctionName);
			functionMessageUpdate.put("functionname", inAutomationStep.getEnabledId());
			functionMessageUpdate.put("date", DateStorageUtil.getStorageUtil().getJsonFormat().format(new Date()));
			Boolean messagereload = (Boolean) inContext.getContextValue("messagereload");

			if (messagereload != null && messagereload.booleanValue())
			{
				functionMessageUpdate.put("command", "messagereload");
			}

			ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");

			JSONObject jsonMessage = new JSONObject(functionMessageUpdate);

			server.broadcastMessage(jsonMessage);
		}
		catch (Exception ex)
		{
			log.error("Error in fireStatusComplete", ex);
		}

		if (inContext.getCurrentAgentJob() != null)
		{
			Long wait = inContext.getWaitTime();
			if (wait != null)
			{
				inContext.setWaitTime(null);
				log.info("Previous function requested to wait " + wait + " milliseconds");
				try
				{
					Thread.sleep(wait);
				}
				catch (InterruptedException ex)
				{
					log.warn("Sleep interrupted", ex);
					Thread.currentThread().interrupt();
				}
			}
			if (response != null)
			{
				String runFunctionName = response.getExecAutomationStep();
				if (runFunctionName != null)
				{
					runProcess(inContext, runFunctionName);
				}
			}
		}
	}

}
