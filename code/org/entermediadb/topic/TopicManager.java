package org.entermediadb.topic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

public class TopicManager extends BaseMediaModule
{

	public void getUserTopics(WebPageRequest inReq)
	{
		MediaArchive mediaArchive = getMediaArchive(inReq);

		Collection<MultiValued> topics = mediaArchive.query("entitytopic").all().search();

		String userid = inReq.getUser().getId();
		Collection<MultiValued> usertopics = new ArrayList<>();
		for (MultiValued topic : topics)
		{
			Collection<String> assignedto = topic.getValues("assignedto");
			if (assignedto != null && assignedto.contains(userid))
			{
				Collection<MultiValued> tutorials = mediaArchive.query("entitytutorial").exact("entitytopic", topic.getId()).search();
				topic.setValue("tutorials", tutorials != null ? tutorials.size() : 0);
				usertopics.add(topic);
			}
		}
		inReq.putPageValue("usertopics", usertopics);
	}

	public void getTopicTutorials(WebPageRequest inReq)
	{
		String topicid = inReq.getRequestParameter("entitytopic");
		MediaArchive mediaArchive = getMediaArchive(inReq);

		Collection<MultiValued> alltutorials = mediaArchive.query("entitytutorial").exact("entitytopic", topicid).search();

		Collection<Map<String, MultiValued>> data = new ArrayList<>();

		for (MultiValued tutorial : alltutorials)
		{
			Map<String, MultiValued> tutorialmap = new HashMap<>();
			tutorialmap.put("tutorial", tutorial);
			MultiValued progress = (MultiValued) mediaArchive.query("tutorialprogress").exact("entitytutorial", tutorial.getId()).exact("user", inReq.getUser().getId()).searchOne();
			tutorialmap.put("progress", progress);
			data.add(tutorialmap);
		}
		inReq.putPageValue("data", data);
	}

	public void loadTutorial(WebPageRequest inReq)
	{
		String tutorialid = inReq.getRequestParameter("entitytutorial");
		MediaArchive mediaArchive = getMediaArchive(inReq);

		MultiValued tutorial = (MultiValued) mediaArchive.query("entitytutorial").exact("id", tutorialid).searchOne();
		inReq.putPageValue("tutorial", tutorial);

		MultiValued progress = (MultiValued) mediaArchive.query("tutorialprogress").exact("entitytutorial", tutorial.getId()).exact("user", inReq.getUser().getId()).searchOne();
		inReq.putPageValue("progress", progress);

		Collection<MultiValued> sections =
			mediaArchive.query("componentsection").exact("playbackentitymoduleid", "entitytutorial").exact("playbackentityid", tutorial.getId()).sort("ordering").search();

		// Collection<Map> sectiondata = new ArrayList<>();

		// for (MultiValued section : sections)
		// {
		// Map sectionmap = new HashMap<>();

		// sectionmap.put("section", section);

		// Collection<MultiValued> componentcontents =
		// mediaArchive.query("componentcontent").exact("componentsectionid",
		// section.getId()).sort("ordering").search();

		// sectionmap.put("componentcontents", componentcontents);

		// sectiondata.add(sectionmap);
		// }

		inReq.putPageValue("sections", sections);
	}

}
