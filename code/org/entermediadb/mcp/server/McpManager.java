package org.entermediadb.mcp.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.agentjobs.AgentJob;
import org.entermediadb.ai.agentjobs.AgentJobManager;
import org.entermediadb.ai.automation.AutomationManager;
import org.entermediadb.ai.llm.VelocityRenderUtil;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.jsonrpc.JsonRpcResponseBuilder;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class McpManager implements CatalogEnabled
{
    private static final Log log = LogFactory.getLog(McpManager.class);

    protected static final String GET_JOB_STATUS = "get_job_status";

    protected ModuleManager fieldModuleManager;
    protected VelocityRenderUtil fieldRender;
    protected String fieldCatalogId;
    protected Map<String, McpConnection> connections = new ConcurrentHashMap<>();

    protected McpGetHandlerManager fieldMcpGetHandlerManager;

    public McpGetHandlerManager getMcpGetHandlerManager()
    {
        if (fieldMcpGetHandlerManager == null)
        {
            fieldMcpGetHandlerManager = new McpGetHandlerManager();
        }
        return fieldMcpGetHandlerManager;
    }

    public void setMcpGetHandlerManager(McpGetHandlerManager inMcpGetHandlerManager)
    {
        fieldMcpGetHandlerManager = inMcpGetHandlerManager;
    }

    public ModuleManager getModuleManager()
    {
        return fieldModuleManager;
    }

    public void setModuleManager(ModuleManager inModuleManager)
    {
        fieldModuleManager = inModuleManager;
    }

    protected AutomationManager fieldAutomationManager;

    public AutomationManager getAutomationManager()
    {
        return (AutomationManager) getModuleManager().getBean(getCatalogId(),"automationManager");
    }

    public void setAutomationManager(AutomationManager inAutomationManager)
    {
        fieldAutomationManager = inAutomationManager;
    }

    public VelocityRenderUtil getRenderUtil()
    {
        return fieldRender;
    }

    public void setRenderUtil(VelocityRenderUtil inRender)
    {
        fieldRender = inRender;
    }

    public MediaArchive getMediaArchive()
    {
        return (MediaArchive) getModuleManager().getBean(getCatalogId(),    "mediaArchive");
    }

    public         AgentJobManager getAgentJobManager()
    {
        return (AgentJobManager) getModuleManager().getBean(getCatalogId(), "agentJobManager", true);
    }


    public String getCatalogId()
    {
        return fieldCatalogId;
    }

    public void setCatalogId(String inCatalogId)
    {
        fieldCatalogId = inCatalogId;
    }

    /**
     * Opens a new SSE connection for the session in inReq if none exists.
     */
    public McpConnection createConnection(MediaArchive inArchive, WebPageRequest inReq)
    {
        String requestedSessionId = inReq.getRequest().getHeader("mcp-session-id");
        if (requestedSessionId == null || requestedSessionId.isEmpty())
        {
            requestedSessionId = inReq.getRequest().getParameter("sessionId");
        }

        String sessionId = (requestedSessionId != null && !requestedSessionId.isEmpty()) ? requestedSessionId : createSessionId();
        String endpoint = inReq.findPathValue("mcp-endpoint");
        // This is something like /sse/userkey
        String key = inReq.getPage().getPageName();
        McpConnection stale = connections.remove(sessionId);
        if (stale != null)
        {
            stale.close();
            log.info("Replacing stale MCP connection for session: " + sessionId);
        }

        McpConnection conn = new McpConnection(inReq);
        conn.setSessionId(sessionId);
        connections.put(sessionId, conn);
        conn.connect();
        conn.openStream(endpoint);

        conn.setKey(key);

        Data row = inArchive.query("appkeys").exact("key", key).searchOne();
        if (row != null)
        {
            String userid = row.get("user");
            User user = inArchive.getUser(userid);
            conn.setUser(user);
        }

        log.info("Created MCP connection for session: " + sessionId);

        try
        {
            // this blocks until conn.active == false
            conn.run();
        }
        finally
        {
            // guarantee we remove it—even on exceptions
            connections.remove(sessionId);
            log.info("Cleaned up MCP connection for session: " + sessionId);
            // ensure the socket is closed if not already
            if (conn.isActive())
            {
                conn.close();
            }
        }

        return conn;

    }

    public void handleCall(WebPageRequest inReq, McpConnection inConnection, String cmd, JSONObject payload) throws Exception
    {
        if (inConnection == null)
        {
            throw new OpenEditException("No active MCP connection for command: " + cmd);
        }

        String response = buildResponse(inReq, cmd, payload);
        inConnection.sendMessage(response);
    }

    /**
     * Builds the JSON-RPC response for a call. Used by both the SSE and the Streamable HTTP transports.
     */
    public String buildResponse(WebPageRequest inReq, String cmd, JSONObject payload) throws Exception
    {
        Object id = payload != null ? payload.get("id") : null;

        inReq.putPageValue("id", id);
        String appid = inReq.findPathValue("applicationid");
        UserProfile profile = inReq.getUserProfile();
        JSONObject params = payload != null ? (JSONObject) payload.get("params") : null;
        String response;

        if ("logging/setLevel".equals(cmd))
        {
            response = new JsonRpcResponseBuilder(id).withServer("eMedia Live").build();
        }
        else
            if ("tools/list".equals(cmd))
            {
                if (profile == null)
                {
                    response = new JsonRpcResponseBuilder(id).withResponse("Authentication failed! User profile not found.", true).build();
                }
                else
                {
                    //"enabledautomation";enabledautomation

                    Collection automations = getMediaArchive().query("automationscenario").exact("connectedtop", "eme_chat").cachedSearch();
                    inReq.putPageValue("enabledautomation", automations);

                    String fp = "/" + appid + "/ai/mcp/method/tools/list.json";
                    inReq.putPageValue("modules", profile.getEntities());

                    String toolsArrString = getRenderUtil().loadInputFromTemplate(inReq, fp);

                    response = new JsonRpcResponseBuilder(id).withToolsList(toolsArrString).build();
                }
            }
            else
                if ("tools/call".equals(cmd))
                {
                    String automationid = params != null ? (String) params.get("name") : null;
                    if (automationid == null || automationid.isEmpty())
                    {
                        response = new JsonRpcResponseBuilder(id).withResponse("Invalid tools/call request. Missing tool name.", true).build();
                    }
                    else
                        if (GET_JOB_STATUS.equals(automationid))
                        {
                            Map arguments = params != null ? (Map) params.get("arguments") : null;
                            String jobid = arguments != null ? (String) arguments.get("jobid") : null;
                            response = buildJobStatusResponse(inReq, id, jobid);
                        }
                        else
                        {
                            Map arguments = params != null ? (Map) params.get("arguments") : null;
                            String query = arguments != null ? (String) arguments.get("query") : null;

                            AgentJob job = getAgentJobManager().createAgentJobFromMessage(inReq.getUserName(), query, automationid, null);

                            //Start running the job. The client polls get_job_status for the result
                            getAgentJobManager().checkQueue();

                            String text = "Job queued. jobid: " + job.getId() + "\nCall the " + GET_JOB_STATUS + " tool with this jobid to get the result.";
                            response = new JsonRpcResponseBuilder(id).withResponse(text, false).build();
                        }
                }
                else
                {
                    log.info("Called " + cmd); // "notifications/initialized"
                    response = new JsonRpcResponseBuilder(id).withResponse("CMD Received " + cmd, false).build();
                }

        return response;
    }

    /**
     * Reports the status of an agent job started by tools/call, with its last response once there is one.
     */
    protected String buildJobStatusResponse(WebPageRequest inReq, Object id, String inJobId)
    {
        if (inJobId == null || inJobId.isEmpty())
        {
            return new JsonRpcResponseBuilder(id).withResponse("Missing jobid.", true).build();
        }
        AgentJob job = (AgentJob) getMediaArchive().getCachedData("agentjob", inJobId);
        if (job == null || !String.valueOf(inReq.getUserName()).equals(job.get("owner")))
        {
            return new JsonRpcResponseBuilder(id).withResponse("Job not found: " + inJobId, true).build();
        }
        String status = job.get("status");
        StringBuffer text = new StringBuffer();
        text.append("jobid: ").append(inJobId).append("\nstatus: ").append(status);
        String last = job.findLastResponse();
        if (last != null)
        {
            text.append("\n\n").append(last);
        }
        else
            if (!"complete".equals(status) && !"error".equals(status))
            {
                text.append("\nStill working. Call ").append(GET_JOB_STATUS).append(" again later.");
            }
        return new JsonRpcResponseBuilder(id).withResponse(text.toString(), "error".equals(status)).build();
    }

    /**
     * Welcome text sent in the initialize response. Override with the mcp-instructions catalog setting.
     */
    public String getInstructions()
    {
        String text = getMediaArchive().getCatalogSettingValue("mcp-instructions");
        if (text == null || text.isEmpty())
        {
            text = "Welcome to EME Live! When the user first connects, greet them warmly and briefly mention what you can help with using this server's tools."
                    + " Tools that start a job return a jobid right away; call " + GET_JOB_STATUS + " with that jobid until the status is complete to get the result.";
        }
        return text;
    }

    public String createSessionId()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * Retrieves the existing connection for the session in inReq, or null if none.
     */
    public McpConnection getConnection(String sessionId)
    {

        // String sessionId = inReq.findValue("sessionId");
        return connections.get(sessionId);
    }

    public McpGetHandler loadGetHandler(WebPageRequest inReq)
    {
        McpGetHandler handler = getMcpGetHandlerManager().loadGetHandler(inReq);
        return handler;
    }

    /**
     * Removes and closes the connection for the given session ID.
     */
    public void removeConnection(String inSessionId)
    {
        McpConnection conn = connections.remove(inSessionId);
        if (conn != null)
        {
            conn.close();
            log.info("Removed MCP connection for session: " + inSessionId);
        }
    }

    /**
     * Scans and removes any inactive or expired connections.
     */
    public void cleanupExpiredConnections()
    {
        Iterator<Map.Entry<String, McpConnection>> it = connections.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, McpConnection> entry = it.next();
            if (!entry.getValue().isActive())
            {
                it.remove();
                log.info("Cleaned up expired MCP connection for session: " + entry.getKey());
            }
        }
    }
}
