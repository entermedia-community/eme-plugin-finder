package org.entermediadb.ai.agentjobs;

import java.util.ArrayList;
import java.util.Collection;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;

public class AgentJob extends BaseData 
{
    private Collection<MultiValued> steps = new ArrayList<MultiValued>();
	public void addStep(MultiValued step)
	{
		steps.add(step);
	}

    public Collection<MultiValued> getSteps()
	{
		return steps;
	}

	public void setSteps(Collection<MultiValued> inSteps)
	{
		steps = inSteps;
	}
}
