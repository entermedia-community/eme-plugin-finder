package org.entermediadb.ai.agentjobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.Skill;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.util.JsonUtil;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;

public class AgentJob extends BaseData implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(AgentJob.class);

	String fieldCatalogId;

	@Override
	public void setCatalogId(String inId)
	{
		fieldCatalogId =inId;	
	}
	ModuleManager fieldModuleManager;
	
	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}	


	private Collection<AgentJobStep> steps;
	private Collection<AgentJobStep> allsteps;
	protected int fieldRunDepth;

	/** How many runProcess calls are running this job right now. The outermost one finishes the job */
	public int getRunDepth()
	{
		return fieldRunDepth;
	}

	public void setRunDepth(int inRunDepth)
	{
		fieldRunDepth = inRunDepth;
	}

	public String getScenarioId()
	{
		return get("automationscenario");
	}

	public MultiValued getScenarioData()
	{
		String scenarioid = getScenarioId();
		if (scenarioid == null)
		{
			return null;
		}
		return (MultiValued) getMediaArchive().getCachedData("automationscenario", scenarioid);
	}

	public void addStep(AgentJobStep inStep)
	{
		getSteps().add(inStep);
		getAllSteps().add(inStep);
	}

	/**
	 * The top level steps of the job. Steps with a "runafter" are children of that step.
	 */
	public Collection<AgentJobStep> getSteps()
	{
		if (steps == null)
		{
			loadSteps();
		}
		return steps;
	}

	/** Every step, each followed by the steps that run after it */
	public Collection<AgentJobStep> getAllSteps()
	{
		if (allsteps == null)
		{
			loadSteps();
		}
		return allsteps;
	}

	protected void loadSteps()
	{
		Collection<MultiValued> found = getMediaArchive().query("agentjobstep").exact("agentjob", getId()).search();
		Map<String, AgentJobStep> byid = new LinkedHashMap<String, AgentJobStep>();
		for (MultiValued data : found)
		{
			AgentJobStep step = new AgentJobStep();
			step.setAgentJobStepData(data);
			String aiskillid = data.get("aiskillid");
			if (aiskillid != null)
			{
				MultiValued agentdata = (MultiValued) getMediaArchive().getCachedData("aiskill", aiskillid);
				String bean = agentdata == null ? null : agentdata.get("bean");
				if (bean == null)
				{
					//Skip it like the old scenario loader did, so its children still run as top level steps
					log.error("Could not find aiskill bean " + aiskillid + " for agentjobstep " + data.getId());
					continue;
				}
				step.setAgentData(agentdata);
				step.setAgent(loadSkill(bean));
			}
			addContextValues(step);
			byid.put(data.getId(), step);
		}
		Collection<AgentJobStep> roots = new ArrayList<AgentJobStep>();
		for (AgentJobStep step : byid.values())
		{
			AgentJobStep parent = step.getParentAgent() == null ? null : byid.get(step.getParentAgent());
			if (parent == null)
			{
				roots.add(step);
			}
			else
			{
				parent.addChild(step);
			}
		}
		steps = roots;
		allsteps = new ArrayList<AgentJobStep>();
		addInRunOrder(roots, allsteps);
	}

	protected void addInRunOrder(Collection<AgentJobStep> inSteps, Collection<AgentJobStep> inOrdered)
	{
		for (AgentJobStep step : inSteps)
		{
			inOrdered.add(step);
			addInRunOrder(step.getChildren(), inOrdered);
		}
	}

	public Skill loadSkill(String inBean)
	{
		Skill skill = (Skill) getMediaArchive().getCacheManager().get("ai", "Agent" + inBean);
		if (skill == null)
		{
			skill = (Skill) fieldModuleManager.getBean(fieldCatalogId, inBean);
			getMediaArchive().getCacheManager().put("ai", "Agent" + inBean, skill);
		}
		return skill;
	}

	protected void addContextValues(AgentJobStep inStep)
	{
		String text = inStep.getAgentJobStepData().get("contextvalues");
		if (text == null && inStep.getAgentData() != null)
		{
			text = inStep.getAgentData().get("contextvalues");
		}
		if (text == null)
		{
			return;
		}
		JSONParser parser = new JSONParser();
		text = text.trim();
		JSONObject json;
		if (text.startsWith("["))
		{
			// [{"llmprompt":"..."},{...}] merges into one set of values
			json = new JSONObject();
			for (Object item : parser.parseJSONArray(text))
			{
				json.putAll((Map) item);
			}
		}
		else
		{
			json = parser.parse(text);
		}
		inStep.setExtraContextValues(json);
	}

	/** Finds a step by its automationstep id (the chat function name) or its own agentjobstep id */
	public AgentJobStep findEnabled(String inEnabledId)
	{
		return findEnabled(getSteps(), inEnabledId);
	}

	public AgentJobStep findEnabled(Collection<AgentJobStep> inSteps, String inEnabledId)
	{
		for (AgentJobStep step : inSteps)
		{
			if (inEnabledId.equals(step.getEnabledId()) || inEnabledId.equals(step.getId()))
			{
				return step;
			}
			AgentJobStep found = findEnabled(step.getChildren(), inEnabledId);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	/** Finds the first step that runs the given aiskill, so copied scenarios with their own step ids still work */
	public AgentJobStep findStepBySkill(String inAiSkill)
	{
		return findStepBySkill(getSteps(), inAiSkill);
	}

	public AgentJobStep findStepBySkill(Collection<AgentJobStep> inSteps, String inAiSkill)
	{
		for (AgentJobStep step : inSteps)
		{
			if (inAiSkill.equals(step.get("aiskillid")))
			{
				return step;
			}
			AgentJobStep found = findStepBySkill(step.getChildren(), inAiSkill);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	public MediaArchive getMediaArchive()
	{
		return (MediaArchive) fieldModuleManager.getBean(fieldCatalogId, "mediaArchive");
	}

	/** Pass null to reload the steps from the database */
	public void setSteps(Collection<AgentJobStep> inSteps)
	{
		steps = inSteps;
		allsteps = inSteps == null ? null : new ArrayList<AgentJobStep>(inSteps);
	}

	/**
	 * The opencode form a step is waiting on ({id, title, fields:[{key, type, title, ...}]}), or null.
	 */
	public Map getPendingForm(AgentJobStep inStep)
	{
		return getPendingForm(inStep.getAgentJobStepData());
	}

	public Map getPendingForm(MultiValued inStep)
	{
		String json = inStep.get("pendingform");
		if (json == null || json.isEmpty())
		{
			return null;
		}
		return new JSONParser().parseMap(json);
	}

	public String findLastResponse()
	{
		setSteps(null); //Pull from database
		ArrayList<AgentJobStep> reversed = new ArrayList<AgentJobStep>(getAllSteps());
		java.util.Collections.reverse(reversed);
		for(AgentJobStep step : reversed)
		{
			MultiValued refreshedstep = (MultiValued) getMediaArchive().getCachedData("agentjobstep", step.getId());
			String lastresponse = refreshedstep.get("markdowncontent");
			if(lastresponse != null)
			{
				return lastresponse;
			}
		}	
		return null;
	}

/*

"type":"step_start","timestamp":1789512487122,"sessionID":"ses_f58be861affeXndBz5ovsVDMms","part":{"id":"prt_0a74190c5001wE9mE0Hfpbe1BO","messageID":"msg_0a7417e7a0016KFWxQ4PREnns0","sessionID":"ses_f58be861affeXndBz5ovsVDMms","snapshot":"c38236e41ebdd89818fa03d8f43b3c3ec4774c22","type":"step-start"}}
{"type":"tool_use","timestamp":1789512488222,"sessionID":"ses_f58be861affeXndBz5ovsVDMms","part":{"type":"tool","tool":"bash","callID":"6Xe5SVwnIIZqL25tCLdmVOcYv1zqgDrS","state":{"status":"completed","input":{"command":"ls -la bin/"},"output":"total 76\ndrwxrwxr-x  4 shanti shanti 4096 Sep 15 12:02 .\ndrwxrwxr-x 11 shanti shanti 4096 Sep 15 12:02 ..\n-rwxrwxr-x  1 shanti shanti  182 Aug 10 11:13 bash.sh\ndrwxrwxr-x  2 shanti shanti 4096 Aug 13 10:45 build\n-rwxrwxr-x  1 shanti shanti  746 Aug 12 12:30 compile.sh\n-rwxrwxr-x  1 shanti shanti 9661 Sep 15 12:02 eme
*/

	public String summaryLog()
	{
		String json = findLastResponse();
		if( json == null )
		{
			return null;
		}
		//Only include json that that starts  with { and ends with }
		StringBuffer cleanJson = new StringBuffer();

		json.lines().forEach(line -> {
			if(line.startsWith("{") && line.endsWith("}")) {
				cleanJson.append(line);
				cleanJson.append("\n");
			}
		});

		Collection<Map> parsed = new JSONParser().parseCollection("[" + cleanJson.toString() + "]");

		StringBuffer clean  = new StringBuffer();

		for(Map map : parsed)
		{
			// Process each map as needed
			String type = (String) map.get("type");
			if( type.equals("tool_use"))
			{
				Map input = (Map) JsonUtil.getObjectFromMaps("part.state.input", map);
				String stringoutput = (String) JsonUtil.getObjectFromMaps("part.state.output", map);
				clean.append("**Input:**\n").append(input.toString()).append("\n");	
				clean.append("**Output:**\n").append(stringoutput).append("\n");
			}
		}

		return clean.toString();
	}
}
