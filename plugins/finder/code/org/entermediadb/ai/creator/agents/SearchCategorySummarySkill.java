package org.entermediadb.ai.creator.agents;

import java.util.Collection;

import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.creator.ChatSmartCreatorConfirmationSkill;
import org.entermediadb.ai.creator.SmartCreatorPlaybackSkill;
import org.entermediadb.ai.AgentContext;

public class SearchCategorySummarySkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		summarizeLatestContentInSearchCategories(inContext);
		super.process(inContext);
	}

	// Auto content creation
	public void summarizeLatestContentInSearchCategories(AgentContext inReq)
	{
		// search for ones with an output type
		// lastsummary_date
		SmartCreatorPlaybackSkill creatorManager = (SmartCreatorPlaybackSkill) getMediaArchive().getBean("SmartCreatorPlaybackSkill");

		Collection categories = getMediaArchive().query("searchcategory").exact("lastsummary_enabled", true).search();
		creatorManager.createContentFromSearchCategories(categories);
		// What kind of content

	}

}
