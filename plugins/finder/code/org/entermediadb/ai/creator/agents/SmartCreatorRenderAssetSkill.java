package org.entermediadb.ai.creator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.creator.SmartCreatorSkill;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.AgentContext;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class SmartCreatorRenderAssetSkill extends BaseSkill
{
	public SmartCreatorSkill getSmartCreatorSkill()
	{
		SmartCreatorSkill manager = (SmartCreatorSkill) getMediaArchive().getBean("smartCreatorSkill");
		return manager;
	}

	/**
	 * Calls render to html Attaches and asset version sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext)
	{
		MultiValued entity = inContext.getCurrentEntity();

		if (entity != null)
		{
			// render html and save to asset
			MultiValued entitymodule = inContext.getCurrentEntityModule();
			String applicationid = (String) inContext.getContextValue("triggerapplicationid");
			String cdnprefix = (String) inContext.getContextValue("triggersiteroot");
			String html = getSmartCreatorSkill().renderToHtml(cdnprefix, applicationid, entitymodule, entity);
			getSmartCreatorSkill().exportAsAsset(inContext, entitymodule, entity, html);
		}
		super.process(inContext);
	}

}
