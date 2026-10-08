package org.entermediadb.ai.agentjobs;

import java.util.ArrayList;
import java.util.Collection;

import org.entermediadb.ai.Skill;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;

/**
 * One agentjobstep row of an AgentJob, with its aiskill and the Skill bean that runs it. Steps copied
 * from an automationscenario keep the automationstep id, and "runafter" links each step to its parent.
 */
public class AgentJobStep
{
	AgentJobStep fieldParentStep;

	public AgentJobStep getParentStep()
	{
		return fieldParentStep;
	}

	public void setParentStep(AgentJobStep inParentStep)
	{
		fieldParentStep = inParentStep;
	}

	MultiValued fieldAgentData;

	/** The aiskill row, or null for steps without a skill (such as opencode steps) */
	public MultiValued getAgentData()
	{
		return fieldAgentData;
	}

	public void setAgentData(MultiValued inAgentData)
	{
		fieldAgentData = inAgentData;
	}

	public Object getValue(String inField)
	{
		Object value = getAgentJobStepData().getValue(inField);
		if (value == null && getAgentData() != null)
		{
			value = getAgentData().getValue(inField);
		}
		return value;
	}

	public String get(String inField)
	{
		String value = getAgentJobStepData().get(inField);
		if (value == null && getAgentData() != null)
		{
			value = getAgentData().get(inField);
		}
		return value;
	}

	public void setValue(String inField, Object inValue)
	{
		getAgentJobStepData().setValue(inField, inValue);
	}

	MultiValued fieldAgentJobStepData;

	/** The agentjobstep row */
	public MultiValued getAgentJobStepData()
	{
		return fieldAgentJobStepData;
	}

	public void setAgentJobStepData(MultiValued inAgentJobStepData)
	{
		fieldAgentJobStepData = inAgentJobStepData;
	}

	public String getId()
	{
		return getAgentJobStepData().getId();
	}

	public String getName()
	{
		return getAgentJobStepData().getName();
	}

	public Skill getAgent()
	{
		return fieldAgent;
	}

	public void setAgent(Skill inAgent)
	{
		fieldAgent = inAgent;
	}

	Skill fieldAgent;

	/** The id of the agentjobstep this one runs after */
	public String getParentAgent()
	{
		return getAgentJobStepData().get("runafter");
	}

	Collection<AgentJobStep> fieldChildren;

	public Collection<AgentJobStep> getChildren()
	{
		if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
		}
		return fieldChildren;
	}

	public void setChildren(Collection<AgentJobStep> inChildren)
	{
		fieldChildren = inChildren;
	}

	public AgentJobStep getChildren(String inId)
	{
		for (AgentJobStep child : getChildren())
		{
			if (inId.equals(child.getEnabledId()) || inId.equals(child.getId()))
			{
				return child;
			}
		}
		return null;
	}

	public void addChild(AgentJobStep inChildAgent)
	{
		getChildren().add(inChildAgent);
		inChildAgent.setParentStep(this);
	}

	@Override
	public String toString()
	{
		return String.valueOf(getAgentData());
	}

	Collection<JSONObject> fieldAgentParameterStructure;

	public Collection<JSONObject> getAgentParameterStructure()
	{
		return fieldAgentParameterStructure;
	}

	public void setAgentParameterStructure(Collection<JSONObject> inAgentParameterStructure)
	{
		fieldAgentParameterStructure = inAgentParameterStructure;
	}

	JSONObject fieldAgentParameterValues;

	public JSONObject getAgentParameterValues()
	{
		return fieldAgentParameterValues;
	}

	public void setAgentParameterValues(JSONObject inAgentParameterValues)
	{
		fieldAgentParameterValues = inAgentParameterValues;
	}

	protected JSONObject fieldExtraContextValues;

	public JSONObject getExtraContextValues()
	{
		return fieldExtraContextValues;
	}

	public void setExtraContextValues(JSONObject inExtraContextValues)
	{
		fieldExtraContextValues = inExtraContextValues;
	}

	/**
	 * The automationstep id this step was copied from, used as the function name in chats. Steps added
	 * directly to a job use their own id.
	 */
	public String getEnabledId()
	{
		String automationstep = getAgentJobStepData().get("automationstep");
		if (automationstep != null)
		{
			return automationstep;
		}
		return getAgentJobStepData().getId();
	}

	public AgentJobStep getNextAutomationStep()
	{
		if (getChildren() != null && getChildren().size() > 0)
		{
			return getChildren().iterator().next();
		}
		return null;
	}
}
