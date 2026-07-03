package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.markdown.MarkdownUtil;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.data.Searcher;

public class SmartCreatorCreateQuestionsSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(SmartCreatorCreateQuestionsSkill.class);

	private static final String[] QUESTION_CHOICES = {"option_a", "option_b", "option_c", "option_d"};

	public ChatSmartCreatorConfirmationSkill getSmartCreatorSkill()
	{
		ChatSmartCreatorConfirmationSkill smartCreatorManager = (ChatSmartCreatorConfirmationSkill) getMediaArchive().getBean("smartCreatorSkill");
		return smartCreatorManager;
	}

	@Override
	public void process(AgentContext inContext)
	{
		makeQuestionsFromContents(inContext);
		super.process(inContext);
	}

	public void makeQuestionsFromContents(AgentContext inContext)
	{
		String playbackentitymoduleid = (String) inContext.getContextValue("playbackentitymoduleid");
		String playbackentityid = (String) inContext.getContextValue("playbackentityid");

		Collection<Data> componentsSection =
			getMediaArchive().getSearcher("componentsection").query().exact("playbackentitymoduleid", playbackentitymoduleid).exact("playbackentityid", playbackentityid).search();

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
		Searcher questionsearcher = getMediaArchive().getSearcher("entityquestion");

		for (Data section : componentsSection)
		{
			String sectionid = section.getId();
			Searcher contentSearcher = getMediaArchive().getSearcher("componentcontent");
			Collection<Data> componentcontents = contentSearcher.query().exact("componentsectionid", sectionid).search();

			String content = "";
			for (Data componentcontent : componentcontents)
			{
				content += componentcontent.get("content");
			}

			inContext.putContextValue("content", content);
			LlmResponse response = llmconnection.callStructure(inContext, "smartcreator_questions");
			JSONObject rawResponse = response.getMessageStructured();
			Collection<Map> questions = (Collection<Map>) rawResponse.get("questions");

			int ordering = componentcontents.size();
			for (Map questionmap : questions)
			{
				String questiontext = (String) questionmap.get("question");
				List<String> choices = (List<String>) questionmap.get("choices");
				int correctindex = (int) questionmap.get("correct_answer_index");

				if (questiontext == null || questiontext.trim().length() == 0 || choices == null || choices.size() < 4 || correctindex < 0 || correctindex >= choices.size())
				{
					log.info("Skipping question because it is missing text or choices: " + questionmap);
					continue;
				}

				Data question = questionsearcher.createNewData();
				question.setValue("question", questiontext);
				for (int i = 0; i < QUESTION_CHOICES.length; i++)
				{
					question.setValue(QUESTION_CHOICES[i], choices.get(i));
				}
				question.setValue("correctoption", QUESTION_CHOICES[correctindex]);
				questionsearcher.saveData(question, inContext.getUserProfile());

				Data componentSection = contentSearcher.createNewData();
				componentSection.setValue("componenttype", "mcq");
				componentSection.setValue("questionid", question.getId());
				componentSection.setValue("modificationdate", new Date());
				componentSection.setValue("componentsectionid", sectionid);
				componentSection.setValue("ordering", ordering++);
				contentSearcher.saveData(componentSection, inContext.getUserProfile());
			}
		}
	}
}
