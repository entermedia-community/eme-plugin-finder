package org.entermediadb.ai.agentjobs;

import java.util.Collection;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.Skill;
import org.entermediadb.ai.llm.BaseAgentContext;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.util.JSONParser;

/**
 * Runs each step's aiskill bean in Java, or its automation scenario when it has no aiskill.
 */
public class JavaSkillOrchestrator extends BaseAgentJobOrchestrator
{
	private static final Log log = LogFactory.getLog(JavaSkillOrchestrator.class);

	protected void processStep(AgentJobRunnable inAgentJob, MultiValued inStep)
	{
		String aiskillid = inStep.get("aiskillid");
		if( aiskillid != null )
		{
			runSkill(inAgentJob,inStep);
			return;
		}
		String automationscenario = inStep.get("automationscenario");
		if( automationscenario != null )
		{
			runScenario(inAgentJob,inStep);
			return;
		}
		log.error("No aiskillid or automationscenario on agentjobstep " + inStep.getId());
	}

	/**
	 * Runs the step's automation scenario. The scenario records its own agentjob with one agentjobstep
	 * per automation step.
	 */
	protected void runScenario(AgentJobRunnable inAgentJob, MultiValued inStep)
	{
		inStep.setValue("status", "running");
		getMediaArchive().saveData("agentjobstep", inStep);

		AgentContext context = (AgentContext) getMediaArchive().getBean("baseAgentContext", false);;
		context.setCatalogId(getCatalogId());
		context.setModuleManager(getModuleManager());
		context.put("agentjob", inAgentJob.getAgentJob());
		context.put("agentjobstep", inStep);
		String userrequest = inStep.get("userrequest");
		if( userrequest == null)
		{
			userrequest = inAgentJob.getAgentJob().get("userrequest");
		}
		context.put("userrequest", userrequest);

		getAgentJobManager().runScenario(inStep.get("automationscenario"), context);

		completeIfRunning(inStep);
	}

	protected void runSkill( AgentJobRunnable inAgentJob, MultiValued inStep)
	{
		String aiskillid = inStep.get("aiskillid");
		Data aiskill = getMediaArchive().query("aiskill").exact("id", aiskillid).searchOne();
		inStep.setValue("status", "running");
		getMediaArchive().saveData("agentjobstep", inStep);

		inAgentJob.getContext().put("agentjobstep",inStep);

		String userrequest = inStep.get("markdowncontent"); //Starting point for each job
		if( inAgentJob.getAgentJob().get("repeatperiod") != null)
		{
			//Skills may replace markdowncontent with their output, so keep the original request for the next repeat
			String original = inStep.get("userrequest");
			if( original == null)
			{
				inStep.setValue("userrequest", userrequest);
			}
			else
			{
				userrequest = original;
			}
		}
		inAgentJob.getContext().put("userrequest", userrequest);

		Skill skill = (Skill) getModuleManager().getBean(getCatalogId(), aiskill.get("bean"));
		String json = inStep.get("parameters");
		Collection<Map<String,Object>> parameters = null;
		if( json != null)
		{
			parameters = (Collection<Map<String,Object>>)new JSONParser().parseCollection(json);
		}

		if(  parameters != null)
		{
			for (Map<String,Object> map : parameters) 
			{
				String key = (String)map.get("input_id");
				String value = (String)map.get("value");
				if( value == null)
				{
					String oldkey  = (String)map.get("variable");
					if( oldkey == null)
					{
						oldkey = key;
					}
					value = (String)inAgentJob.getContext().getContextValue(oldkey);
				}
				if( value == null)
				{
					continue;
				}
				if( value.startsWith("${"))
				{
					String[] parts = value.split("\\.");
					if( parts.length > 1)
					{
						String oldkey = parts[parts.length -1];
						if( oldkey.endsWith("}"))
						{
							oldkey = oldkey.substring(0,oldkey.length() -1);
						}
						value = (String)inAgentJob.getContext().getContextValue(oldkey);
					}
				}

				inAgentJob.getContext().put(key,value);
			}
		}

		skill.process(inAgentJob.getContext());

		completeIfRunning(inStep);
	}
}
