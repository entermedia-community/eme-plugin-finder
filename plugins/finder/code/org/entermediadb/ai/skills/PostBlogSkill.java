package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.AgentContext;
import org.openedit.MultiValued;

public class PostBlogSkill extends BaseSkill
{
	public ChatSmartCreatorConfirmationSkill getSmartCreatorSkill()
	{
		ChatSmartCreatorConfirmationSkill manager = (ChatSmartCreatorConfirmationSkill) getMediaArchive().getBean("smartCreatorSkill");
		return manager;
	}

	/**
	 * Calls render to html Attaches and asset version sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext)
	{
		// MultiValued entity = inContext.getCurrentEntity();
		//
		// if(entity != null)
		// {
		// getSmartCreatorSkill().processRecords(inContext.getScriptLogger(),inContext.getCurrentAgentEnable().getAgentConfig(),pageofhits);;
		// for (Iterator iterator2 = pageofhits.iterator(); iterator2.hasNext();)
		// {
		// MultiValued data = (MultiValued) iterator2.next();
		// if(data.getBoolean("llmerror"))
		// {
		// workinghits.remove(data); //We do not process more.
		// }
		// }
		// mycontext.setRecordsToProcess(workinghits);
		// }
		// super.process(mycontext);
	}

}
