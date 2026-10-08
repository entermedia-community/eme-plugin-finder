package org.entermediadb.ai.agentjobs;

import org.entermediadb.ai.Skill;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

/**
 * Sends each step's request to an opencode session (via openCodeRunnerSkill) instead of running
 * the step's aiskill or automation scenario in Java.
 */
public class OpenCodeOrchestrator extends BaseAgentJobOrchestrator
{
	protected void processStep(AgentJobRunnable inAgentJob, MultiValued inStep)
	{
		String userrequest = findUserRequest(inAgentJob.getAgentJob(), inStep);
		if (userrequest == null)
		{
			throw new OpenEditException("OpenCodeOrchestrator: no request to send to opencode for step " + inStep.getId());
		}
		//openCodeRunnerSkill reads the prompt from the step and keeps it for resumes and repeats
		inStep.setValue("userrequest", userrequest);
		inStep.setValue("status", "running");
		getMediaArchive().saveData("agentjobstep", inStep);

		inAgentJob.getContext().put("agentjobstep", inStep);
		inAgentJob.getContext().put("userrequest", userrequest);

		Skill skill = (Skill) getModuleManager().getBean(getCatalogId(), "openCodeRunnerSkill");
		skill.process(inAgentJob.getContext());

		completeIfRunning(inStep);
	}

	protected String findUserRequest(AgentJob inJob, MultiValued inStep)
	{
		String[] values = new String[] { inStep.get("userrequest"), inStep.get("markdowncontent"), inJob.get("userrequest") };
		for (String value : values)
		{
			if (value != null && !value.trim().isEmpty())
			{
				return value;
			}
		}
		return null;
	}
}
