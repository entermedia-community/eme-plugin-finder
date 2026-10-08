package org.entermediadb.ai.agentjobs;

import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.WebPageRequest;

public class AgentJobModule extends BaseMediaModule
{
    
    public void checkQueue(WebPageRequest inReq)
    {
        String catalogid = inReq.findValue("catalogid");
        AgentJobManager manager = (AgentJobManager) getModuleManager().getBean(catalogid, "agentJobManager", true);
        int count = manager.runningProcesses();
        inReq.putPageValue("runningcount", count);
        manager.checkQueue();

        ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");
        if( logger != null)
        {
            int pendingnew = manager.getNewjobs().size();
            logger.info("Job queue " + count + " jobs are running, " + pendingnew + " new jobs pending");
        }
    }

    public void checkRepeatingJobs(WebPageRequest inReq)
    {
        String catalogid = inReq.findValue("catalogid");
        AgentJobManager manager = (AgentJobManager) getModuleManager().getBean(catalogid, "agentJobManager", true);
        int count = manager.checkRepeatingJobs();

        ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");
        if( logger != null)
        {
            logger.info("Repeating agent jobs " + count + " due to run");
        }
    }

}
