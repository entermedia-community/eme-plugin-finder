package org.entermediadb.ai;

import java.util.Collection;
import java.util.Map;
import org.entermediadb.ai.assistant.AiCreation;
import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LogEntry;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

/**
 * Extract every public method from BaseAgentContext and make it available here. This is the context
 * that will be passed to the agent function and should contain everything the agent needs to know
 * about the current state of the conversation and any relevant data.
 * 
 */

public interface AgentContext extends Data
{
	ScriptLogger getScriptLogger();

	void setScriptLogger(ScriptLogger inScriptLogger);

	AgentContext getParentContext();

	void setParentContext(AgentContext inParentContext);

	AgentContext getRootContext();

	Long getWaitTime();

	void setWaitTime(Long inWaitTime);

	MultiValued getCurrentScenerio();

	void setCurrentScenerio(MultiValued inCurrentScenerio);

	AgentEnabled getCurrentAgentEnable();

	void setCurrentAgentEnable(AgentEnabled inCurrentAgentEnable);

	void setAgentEnableChildren(Collection<AgentEnabled> inAgentEnableChildren);

	void setAgentEnableChildren(AgentEnabled inAgentEnableChild);

	Collection<AgentEnabled> getAgentEnableChildren();

	String getCatalogId();

	void setCatalogId(String inCatalogId);

	ModuleManager getModuleManager();

	void setModuleManager(ModuleManager inModuleManager);

	UserProfile getUserProfile();

	void setUserProfile(UserProfile inUserProfile);

	String get(String inKey);

	String getNextFunctionName();

	void setNextFunctionName(String inNextFunctionName);

	String getTopLevelFunctionName();

	void setTopLevelFunctionName(String inTopLevelFunctionName);

	Map<String, Object> getContext();

	Map<String, Object> getAllContext();

	Data getChannel();

	void setChannel(Data inChannel);

	Object getContextValue(String inKey);

	void setContext(Map<String, Object> inContext);

	void put(String inKey, Object inValue);

	void addContext(String inKey, Object inValue);

	void putContextValue(String inKey, Object inValue);

	String toString();

	Collection<RankedResult> getRankedSuggestions();

	void setRankedSuggestions(Collection<RankedResult> inRankedSuggestions);

	AiSearch getAiSearchParams();

	void setAiSearchParams(AiSearch inAiSearchParams);

	AiCreation getAiCreationParams();

	void setAiCreationParams(AiCreation inAiCreationParams);

	AiSmartCreatorSteps getAiSmartCreatorSteps();

	void setAiSmartCreatorSteps(AiSmartCreatorSteps inAiSmartCreatorSteps);

	String getFunctionName();

	void setFunctionName(String inFunctionName);

	String getMessagePrefix();

	void setMessagePrefix(String inMessagePrefix);

	Collection<String> getExcludedEntityIds();

	void setExcludedEntityIds(Collection<String> inExcludedEntityIds);

	void addExcludedEntityId(String inExcludedEntityId);

	Collection<String> getExcludedAssetIds();

	void setExcludedAssetIds(Collection<String> inExcludedAssetIds);

	void addExcludedAssetId(String inExcludedAssetId);

	User getChatUser();

	void setLocale(String inLocale);

	String getLocale();

	void log(String inMessage);

	void info(String inMessage);

	void error(String inMessage);

	void error(String inMessage, Throwable inThrowable);

	void setCurrentEntityModule(MultiValued inEntityModule);

	void setCurrentEntity(MultiValued inEntity);

	MultiValued getCurrentEntityModule();

	MultiValued getCurrentEntity();

	Collection<LogEntry> getLogs();
}
