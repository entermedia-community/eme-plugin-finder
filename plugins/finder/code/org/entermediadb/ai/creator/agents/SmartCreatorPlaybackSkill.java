package org.entermediadb.ai.creator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.mail.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.creator.SmartCreatorSession;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.llm.BaseAgentContext;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.edit.Version;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.util.Inflector;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.users.User;

public class SmartCreatorPlaybackSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(SmartCreatorPlaybackSkill.class);

	public void process(AgentContext inContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inContext;
		String functionName = inContext.getCurrentAgentEnable().getEnabledId();
		boolean runandreturn = "smartcreator_parsecontent".equals(functionName);
		if (functionName == null || runandreturn)
		{
			parseSection(messageContext);
			if (runandreturn)
			{
				return;
			}
		}
		super.process(inContext);

	}

	/*
	 * public void playOutline(ChatMessageContext messageContext, Instructions instructions) { Data
	 * playbackentity = instructions.getTargetEntity(); messageContext.addContext("playbackentity",
	 * playbackentity);
	 * 
	 * Data playbackentitymodule = instructions.getTargetModule();
	 * messageContext.addContext("playbackentitymodule", playbackentitymodule);
	 * 
	 * String entityid = messageContext.get("entityid"); String entitymoduleid =
	 * messageContext.get("entitymoduleid");
	 * 
	 * LlmConnection llmconnection = getMediaArchive().getLlmConnection(agentFn); LlmResponse response =
	 * llmconnection.renderLocalAction(messageContext, agentFn);
	 * 
	 * return; }
	 */

	public void getCreator(WebPageRequest inReq)
	{
		String playbackentityid = inReq.getRequestParameter("playbackentityid");
		String playbackentitymoduleid = inReq.getRequestParameter("playbackentitymoduleid");

		AgentContext agentContext = (AgentContext) inReq.getPageValue("agentcontext");

		if (playbackentityid == null && agentContext != null)
		{
			playbackentityid = (String) agentContext.getContextValue("playbackentityid");
			playbackentitymoduleid = (String) agentContext.getContextValue("playbackentitymoduleid");
		}

		if (playbackentityid == null)
		{
			playbackentityid = inReq.findValue("entityid");
			playbackentitymoduleid = inReq.findValue("module");
		}

		if (playbackentityid == null || playbackentitymoduleid == null)
		{
			throw new IllegalArgumentException("Missing playbackentityid or playbackentitymoduleid parameter");
		}

		Data playbackentitymodule = getMediaArchive().getCachedData("module", playbackentitymoduleid);
		inReq.putPageValue("playbackentitymodule", playbackentitymodule);

		Data playbackentity = getMediaArchive().getCachedData(playbackentitymoduleid, playbackentityid);
		inReq.putPageValue("playbackentity", playbackentity);

		// Get the Publish Tab View
		Data publishtab = loadPublishView(playbackentitymoduleid);
		inReq.putPageValue("publishtab", publishtab);

		Searcher sectionsearcher = getMediaArchive().getSearcher("componentsection");

		Integer playbacksection = null;

		String secpage = inReq.getRequestParameter("playbacksection");
		if (secpage != null)
		{
			playbacksection = Integer.parseInt(secpage);
		}
		if (playbacksection == null && agentContext != null)
		{
			playbacksection = (Integer) agentContext.getContextValue("playbacksection");
		}

		if (playbacksection != null)
		{
			HitTracker allsections = sectionsearcher.query().exact("playbackentityid", playbackentity.getId()).sort("ordering").search();
			Data selectedsection = allsections.get(playbacksection);
			inReq.putPageValue("selectedsection", selectedsection);

			if (playbacksection - 1 >= 0)
			{
				Data prevsection = allsections.get(playbacksection - 1);
				if (prevsection != null)
				{
					inReq.putPageValue("prevsection", prevsection);
					inReq.putPageValue("previndex", playbacksection - 1);
				}
			}

			if (allsections.size() > playbacksection + 1)
			{
				Data nextsection = allsections.get(playbacksection + 1);
				if (nextsection != null)
				{
					inReq.putPageValue("nextsection", nextsection);
					inReq.putPageValue("nextindex", playbacksection + 1);
				}
			}

		}
		else
		{
			HitTracker hits = loadAllSections(playbackentityid);
			inReq.putPageValue("componentsections", hits);
		}
	}

	public HitTracker loadAllSections(String playbackentityid)
	{
		HitTracker hits = getMediaArchive().query("componentsection").exact("playbackentityid", playbackentityid).sort("ordering").search();
		return hits;
	}

	public Data loadPublishView(String playbackentitymoduleid)
	{
		Data publishtab =
			getMediaArchive().getSearcher("view").query().exact("moduleid", playbackentitymoduleid).exact("systemdefined", "false").exact("rendertype", "tabsmartcreatorpreview").searchOne();
		return publishtab;
	}

	public SmartCreatorSession loadSections(String playbackentitymoduleid, String playbackentityid)
	{

		if (playbackentityid == null || playbackentitymoduleid == null)
		{
			throw new IllegalArgumentException("Missing playbackentityid or playbackentitymoduleid parameter");
		}

		Data playbackentitymodule = getMediaArchive().getCachedData("module", playbackentitymoduleid);

		Data playbackentity = getMediaArchive().getCachedData(playbackentitymoduleid, playbackentityid);

		Searcher sectionsearcher = getMediaArchive().getSearcher("componentsection");

		SmartCreatorSession session = new SmartCreatorSession();

		HitTracker sections = sectionsearcher.query().exact("playbackentitymoduleid", playbackentitymoduleid).exact("playbackentityid", playbackentityid).sort("ordering").search();
		for (Iterator iterator = sections.iterator(); iterator.hasNext();)
		{
			Data section = (Data) iterator.next();
			session.addSection(section);

			HitTracker components = getMediaArchive().getSearcher("componentcontent").query().exact("componentsectionid", section.getId()).sort("ordering").search();
			for (Iterator iterator2 = components.iterator(); iterator2.hasNext();)
			{
				Data component = (Data) iterator2.next();
				session.addComponent(section.getId(), component);
			}
		}
		return session;
	}

	public Data createCreatorSection(Data inPlayback, String inPlaybackModuleId, Map inFields)
	{
		MediaArchive archive = getMediaArchive();

		Searcher sectionsearcher = archive.getSearcher("componentsection");

		String sectionid = (String) inFields.get("sectionid");

		if (sectionid != null)
		{
			Data existing = sectionsearcher.loadData(sectionid);
			existing.setName((String) inFields.get("name"));
			existing.setValue("modificationdate", new Date());
			sectionsearcher.saveData(existing, null);
			return existing;
		}

		int inOrdering = (int) inFields.get("ordering");

		Data section = sectionsearcher.createNewData();

		section.setName("New Section");
		section.setValue("playbackentityid", inPlayback.getId());
		section.setValue("playbackentitymoduleid", inPlaybackModuleId);
		section.setValue("entitymoduleid", inPlayback.get("entitymoduleid"));
		section.setValue("entityid", inPlayback.get("entityid"));
		section.setValue("ordering", inOrdering);
		section.setValue("creationdate", new Date());
		section.setValue("modificationdate", new Date());

		sectionsearcher.saveData(section, null);

		Collection<MultiValued> allSections = sectionsearcher.query().exact("playbackentityid", inPlayback.getId()).moreThan("ordering", inOrdering - 1).search();
		Collection<Data> tosave = new ArrayList<Data>();
		for (Iterator iterator = allSections.iterator(); iterator.hasNext();)
		{
			MultiValued data = (MultiValued) iterator.next();
			if (data.getId().equals(section.getId()))
			{
				continue;
			}
			int currentordering = data.getInt("ordering");
			if (currentordering >= inOrdering)
			{
				data.setValue("ordering", currentordering + 1);
				tosave.add(data);
			}
		}
		sectionsearcher.saveAllData(tosave, null);

		reorderAll(sectionsearcher);

		return section;
	}

	public Data saveComponentContent(String inSectionId, Map inComponents)
	{
		MediaArchive archive = getMediaArchive();
		Searcher contentsearcher = archive.getSearcher("componentcontent");

		String content = (String) inComponents.get("content");
		if (content == null)
		{
			content = "";
		}

		content = content.replaceAll("<p.*>&nbsp;</p>", "\n");
		content = content.replaceAll("<p.*></p>", "\n");
		content = content.replaceAll("^\\s+", "");
		content = content.replaceAll("\\s+$", "");

		Data componentSection = contentsearcher.createNewData();

		String componentcontentid = (String) inComponents.get("componentcontentid");

		if (componentcontentid != null)
		{
			componentSection = contentsearcher.loadData(componentcontentid);
		}

		componentSection.setValue("content", content);
		componentSection.setValue("assetid", inComponents.get("assetid"));
		componentSection.setValue("modificationdate", new Date());

		if (inComponents.get("question") != null)
		{
			createQuestionForContent(componentSection, inComponents);
		}

		if (componentcontentid != null)
		{
			contentsearcher.saveData(componentSection, null);
			return componentSection;
		}

		Collection<MultiValued> allCompononets = contentsearcher.query().exact("componentsectionid", inSectionId).search();

		String orderingStr = (String) inComponents.get("ordering");

		int ordering = -1;

		try
		{
			ordering = Integer.parseInt(orderingStr) + 1;
		}
		catch (Exception e)
		{
			// ignore
		}

		if (ordering < 0)
		{
			ordering = allCompononets.size();
		}

		componentSection.setValue("componentsectionid", inSectionId);
		componentSection.setValue("componenttype", inComponents.get("componenttype"));
		componentSection.setValue("ordering", ordering);
		componentSection.setValue("creationdate", new Date());

		contentsearcher.saveData(componentSection, null);

		Collection<Data> tosave = new ArrayList<Data>();
		for (Iterator iterator = allCompononets.iterator(); iterator.hasNext();)
		{
			MultiValued data = (MultiValued) iterator.next();
			if (data.getId().equals(componentSection.getId()))
			{
				continue;
			}
			int currentordering = data.getInt("ordering");
			if (currentordering >= ordering)
			{
				data.setValue("ordering", currentordering + 1);
				tosave.add(data);
			}
		}
		contentsearcher.saveAllData(tosave, null);

		reorderAll(contentsearcher);

		return componentSection;

	}

	private void createQuestionForContent(Data inComponentSection, Map inComponents)
	{
		Searcher questionsearcher = getMediaArchive().getSearcher("entityquestion");
		Data question = questionsearcher.createNewData();
		if (inComponentSection.get("questionid") != null)
		{
			question = questionsearcher.loadData(inComponentSection.get("questionid"));
		}
		question.setValue("question", inComponents.get("question"));

		question.setValue("mcq", inComponents.get("mcq"));
		question.setValue("option_a", inComponents.get("option_a"));
		question.setValue("option_b", inComponents.get("option_b"));
		question.setValue("option_c", inComponents.get("option_c"));
		question.setValue("option_d", inComponents.get("option_d"));
		question.setValue("mcqcognitivelevel", inComponents.get("mcqcognitivelevel"));
		question.setValue("mcqoptions", inComponents.get("mcqoptions"));
		question.setValue("rationale", inComponents.get("rationale"));
		question.setValue("grade", inComponents.get("grade"));

		questionsearcher.saveData(question, null);

		inComponentSection.setValue("questionid", question.getId());
	}

	public Data duplicateCreatorSection(String inSearchType, String inId)
	{
		Searcher sectionsearcher = getMediaArchive().getSearcher(inSearchType);
		MultiValued section = (MultiValued) sectionsearcher.loadData(inId);

		if (section != null)
		{
			int currentordering = section.getInt("ordering");

			Data newsection = sectionsearcher.createNewData();

			for (Iterator iterator = section.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				if (key.equals("id") || key.startsWith("."))
				{
					continue;
				}
				if ("ordering".equals(key))
				{
					newsection.setValue("ordering", currentordering + 1);
					continue;
				}
				newsection.setValue(key, section.getValue(key));
			}
			sectionsearcher.saveData(newsection, null);

			Collection<MultiValued> all = new ArrayList<MultiValued>();
			if ("componentcontent".equals(inSearchType))
			{
				all = sectionsearcher.query().exact("componentsectionid", section.get("componentsectionid")).search();
			}
			else
				if ("componentsection".equals(inSearchType))
				{
					all = sectionsearcher.query().exact("playbackentityid", section.get("playbackentityid")).search();
				}

			Collection<Data> tosave = new ArrayList<Data>();
			for (Iterator iterator = all.iterator(); iterator.hasNext();)
			{
				MultiValued data = (MultiValued) iterator.next();
				if (data.getId().equals(newsection.getId()))
				{
					continue;
				}
				int ordering = data.getInt("ordering");
				if (ordering >= currentordering)
				{
					data.setValue("ordering", ordering + 1);
					tosave.add(data);
				}
			}
			sectionsearcher.saveAllData(tosave, null);

			reorderAll(sectionsearcher);

			return newsection;
		}

		return null;

	}

	public void orderCreatorSection(WebPageRequest inReq)
	{
		String sourceid = inReq.getRequestParameter("source");
		String targetid = inReq.getRequestParameter("target");
		String sourceorder = inReq.getRequestParameter("sourceorder");
		String targetorder = inReq.getRequestParameter("targetorder");

		String searchtype = inReq.getRequestParameter("searchtype");
		Searcher searcher = getMediaArchive().getSearcher(searchtype);

		Data source = searcher.loadData(sourceid);
		Data target = searcher.loadData(targetid);

		try
		{
			if (source != null)
			{
				source.setValue("ordering", Integer.parseInt(targetorder));
				searcher.saveData(source, inReq.getUser());
			}
			if (target != null)
			{
				target.setValue("ordering", Integer.parseInt(sourceorder));
				searcher.saveData(target, inReq.getUser());
			}

			reorderAll(searcher);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

	}

	public void deleteCreatorSection(String inSearchType, String inId)
	{
		Searcher sectionsearcher = getMediaArchive().getSearcher(inSearchType);
		Data section = sectionsearcher.loadData(inId);
		if (section != null)
		{
			sectionsearcher.delete(section, null);
		}
		reorderAll(sectionsearcher);
	}

	protected void reorderAll(Searcher searcher)
	{
		HitTracker inHits = searcher.query().sort("ordering").search();
		Collection<Data> tosave = new ArrayList<Data>();
		int idx = 0;
		for (Iterator iterator = inHits.iterator(); iterator.hasNext();)
		{
			MultiValued data = (MultiValued) iterator.next();
			data.setValue("ordering", idx);
			tosave.add(data);
			idx++;
		}
		searcher.saveAllData(tosave, null);

	}

	public String renderToHtml(String inCdnPrefix, String inAppHome, MultiValued inEntityModule, MultiValued inEntity)
	{
		AgentContext context = new BaseAgentContext();
		context.setCurrentEntityModule(inEntityModule);
		context.setCurrentEntity(inEntity);
		context.put("playbackentityid", inEntity.getId());
		context.put("playbackentitymoduleid", inEntityModule.getId());

		HitTracker hits = loadAllSections(inEntity.getId());
		context.put("componentsections", hits);

		Data publishtab = loadPublishView(inEntityModule.getId());
		context.put("publishtab", publishtab);

		context.put("module", inEntityModule.getId());
		context.put("searchhome", "/" + inAppHome + "/views/modules/" + inEntityModule.getId() + "/results/default");
		context.put("cdnprefix", inCdnPrefix);

		/// views/agentresponses/smartcreator/index.html
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_renderhtml");
		LlmResponse response = llmconnection.renderLocalAction(context, "smartcreator/renderhtml");
		String got = response.getMessage();
		return got;

	}

	private void exportAsAsset(AgentContext messageContext, String inString)
	{

		Data playbackentitymodule = messageContext.getAiSmartCreatorSteps().getTargetModule();
		Data playbackentity = messageContext.getAiSmartCreatorSteps().getTargetEntity();

		exportAsAsset(messageContext, playbackentitymodule, playbackentity, inString);

	}

	public void exportAsAsset(AgentContext messageContext, Data playbackentitymodule, Data playbackentity, String inHtml)
	{
		String assetsourcepath = getMediaArchive().getEntityManager().loadUploadSourcepath(playbackentitymodule, playbackentity, null);
		assetsourcepath = assetsourcepath + "/" + playbackentity.getName() + ".html";
		Asset asset = (Asset) getMediaArchive().getAssetSearcher().createNewData();
		asset.setSourcePath(assetsourcepath);

		ContentItem original = getMediaArchive().getOriginalContent(asset);
		if (original.exists())
		{
			// ContentItem preview =
			// getMediaArchive().getPresetManager().outPutForGenerated(archive, asset,
			// "image3000x3000");
			getMediaArchive().getAssetEditor().backUpFilesForLastVersion(asset, original, null);
		}

		ContentItem content = new StringItem("/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals/" + assetsourcepath, inHtml, "UTF-8");
		getMediaArchive().getPageManager().getRepository().put(content);

		asset = getMediaArchive().getAssetManager().findAssetSource(asset).createAsset(asset, content, new HashMap(), assetsourcepath, false, messageContext.getChatUser());
		asset.setProperty("importstatus", "created");

		String userid = playbackentity.get("owner");
		if (userid == null && messageContext.getChatUser() != null)
		{
			userid = messageContext.getChatUser().getId();
		}

		User user = getMediaArchive().getUser(userid);

		// add asset to entity
		Category cat = getMediaArchive().getEntityManager().loadDefaultFolder(playbackentitymodule, playbackentity, user);
		asset.addCategory(cat);

		getMediaArchive().saveAsset(asset);

		getMediaArchive().getAssetEditor().createNewVersionData(asset, original, userid, Version.PUBLISHED, null);

		playbackentity.setValue("primarymedia", asset.getId());
		getMediaArchive().getSearcher(playbackentitymodule.getId()).saveData(playbackentity);

		getMediaArchive().fireSharedMediaEvent("importing/assetscreated");

		// getMediaArchive().fireSharedMediaEvent("llm/addmetadata");
	}

	public void correctGrammar(WebPageRequest inReq, String inComponentcontentid)
	{
		Data componentcontent = getMediaArchive().getCachedData("componentcontent", inComponentcontentid);
		String content = componentcontent.get("content");

		if (content == null || content.isEmpty())
		{
			return;
		}

		AgentContext agentcontext = new BaseAgentContext();
		agentcontext.put("paragraph", content);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
		LlmResponse response = llmconnection.callSmartCreatorAiAction(agentcontext, "grammar");

		JSONObject result = response.getMessageStructured();
		if (result != null)
		{
			String corrected_text = (String) result.get("corrected_text");
			if (corrected_text != null)
			{
				inReq.putPageValue("paragraph", corrected_text);
			}
		}

	}

	public void improveContent(WebPageRequest inReq, String inComponentcontentid)
	{
		Data componentcontent = getMediaArchive().getCachedData("componentcontent", inComponentcontentid);
		String content = componentcontent.get("content");

		if (content == null || content.isEmpty())
		{
			return;
		}

		AgentContext agentcontext = new BaseAgentContext();
		agentcontext.put("paragraph", content);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
		LlmResponse response = llmconnection.callSmartCreatorAiAction(agentcontext, "improve");

		JSONObject result = response.getMessageStructured();
		if (result != null)
		{
			String paragraph = (String) result.get("paragraph");
			if (paragraph != null)
			{
				inReq.putPageValue("paragraph", paragraph);
			}
		}
	}

	public void generateContent(WebPageRequest inReq, String inComponentcontentid, String inPrompt)
	{
		Data componentcontent = getMediaArchive().getCachedData("componentcontent", inComponentcontentid);
		String content = componentcontent.get("content");

		if (content == null || content.isEmpty())
		{
			return;
		}

		AgentContext agentcontext = new BaseAgentContext();
		agentcontext.put("prompt", inPrompt);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
		LlmResponse response = llmconnection.callSmartCreatorAiAction(agentcontext, "generate");

		JSONObject result = response.getMessageStructured();
		if (result != null)
		{
			String paragraph = (String) result.get("paragraph");
			if (paragraph != null)
			{
				inReq.putPageValue("paragraph", paragraph);
			}
		}

	}

	public void createImage(WebPageRequest inReq, String inComponentcontentid, String inPrompt)
	{
		// TODO Auto-generated method stub

	}

	public void captionImage(WebPageRequest inReq, String inComponentcontentid, String inAssetid)
	{
		// TODO Auto-generated method stub

	}

	public void createContentFromSearchCategories(Collection inCategories)
	{

	}

	public Collection<Map> parseSection(ChatMessageContext messageContext)
	{
		Data usermessage = getMediaArchive().getCachedData("chatterbox", messageContext.getAgentMessage().get("replytoid"));
		String sectiontext = usermessage.get("message");
		messageContext.addContext("sectiontext", sectiontext);
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_parsecontent");
		LlmResponse response = llmconnection.callStructure(messageContext, "smartcreator_parsecontent");
		JSONObject json = response.getMessageStructured();
		Collection boundaries = (Collection) json.get("parsed_content");
		return boundaries;
	}

}
