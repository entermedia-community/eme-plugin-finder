package org.entermediadb.ai.agentjobs;

import java.util.Collection;
import java.util.Map;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.WebPageRequest;

public class AgentJobModule extends BaseMediaModule
{

    protected AgentJobManager getAgentJobManager(WebPageRequest inReq)
    {
        String catalogid = inReq.findValue("catalogid");
        return (AgentJobManager) getModuleManager().getBean(catalogid, "agentJobManager", true);
    }

    public void checkQueue(WebPageRequest inReq)
    {
        AgentJobManager manager = getAgentJobManager(inReq);
        int count = manager.runningProcesses();
        inReq.putPageValue("runningcount", count);
        manager.checkQueue();

        ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");
        if (logger != null)
        {
            int pendingnew = manager.getNewjobs().size();
            logger.info("Job queue " + count + " jobs are running, " + pendingnew + " new jobs pending");
        }
    }

    public void checkRepeatingJobs(WebPageRequest inReq)
    {
        AgentJobManager manager = getAgentJobManager(inReq);
        
        int count = manager.checkRepeatingJobs();

        ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");
        if (logger != null)
        {
            if (count > 0)
            {
                logger.info("Repeating agent jobs " + count + " due to run");
            }
            else
            {
                logger.info("No repeating agent jobs due to run");
            }
        }
    }

    public void rerunJob(WebPageRequest inReq)
    {
        AgentJobManager manager = getAgentJobManager(inReq);
        String jobid = inReq.getRequestParameter("jobid");
        manager.rerunJob(jobid);
    }

    public void stopJob(WebPageRequest inReq)
    {
        AgentJobManager manager = getAgentJobManager(inReq);
        String jobid = inReq.getRequestParameter("jobid");
        boolean stopped = jobid != null && manager.stopJob(jobid);
        inReq.putPageValue("stopped", stopped);
        inReq.putPageValue("jobid", jobid);
    }

    public void getFilteredJobs(WebPageRequest inReq)
    {
        AgentJobManager manager = getAgentJobManager(inReq);
        manager.getFilteredJobs(inReq);
    }

    public void getAgentJobLogs(WebPageRequest inReq)
    {
        AgentJobManager manager = getAgentJobManager(inReq);
        String jobid = inReq.getRequestParameter("jobid");
        Collection<Map<String, String>> jobLogs = manager.getJobLogs(jobid);
        String since = inReq.getRequestParameter("since"); // epoch millis; only newer entries (live polling)
        if (since != null && since.matches("\\d+"))
        {
            long cutoff = Long.parseLong(since);
            Collection<Map<String, String>> newer = new java.util.ArrayList<>();
            for (Map<String, String> entry : jobLogs)
            {
                String ts = entry.get("timestamp");
                if (ts != null && ts.matches("\\d+") && Long.parseLong(ts) > cutoff)
                {
                    newer.add(entry);
                }
            }
            jobLogs = newer;
        }
        inReq.putPageValue("joblogs", jobLogs);
    }


    public void importRecurringScenario(WebPageRequest inReq)
    {
        AgentJobManager manager = getAgentJobManager(inReq);
        String scenarioId = inReq.getRequestParameter("scenarioid");
        AgentJob job =manager.importRecurringScenario(scenarioId, inReq.findValue("userid"));
    }
}
