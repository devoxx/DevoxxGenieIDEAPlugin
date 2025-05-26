package com.devoxx.genie.service;

import java.util.List;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.LanguageModel;

public interface DevoxxGenieSettingsService {

    List<CustomPrompt> getCustomPrompts();

    List<LanguageModel> getLanguageModels();

    String getOllamaModelUrl();

    String getLmstudioModelUrl();

    String getGpt4allModelUrl();

    String getJanModelUrl();

    String getOpenAIKey();

    String getAzureOpenAIEndpoint();

    String getAzureOpenAIDeployment();

    String getAzureOpenAIKey();

    String getAwsSecretKey();

    String getAwsAccessKeyId();

    String getAwsRegion();

    String getMistralKey();

    String getAnthropicKey();

    String getGroqKey();

    String getDeepInfraKey();

    String getGeminiKey();

    String getDeepSeekKey();

    String getOpenRouterKey();

    Boolean getIsWebSearchEnabled();

    String getGoogleSearchKey();

    String getGoogleCSIKey();

    String getTavilySearchKey();

    Integer getMaxSearchResults();

    String getSelectedProvider(String projectLocation);

    String getSelectedLanguageModel(String projectLocation);

    Boolean getStreamMode();

    Double getTemperature();

    Double getTopP();

    Integer getTimeout();

    Integer getMaxRetries();

    Integer getChatMemorySize();

    Integer getMaxOutputTokens();

    String getSystemPrompt();

    String getTestPrompt();

    String getReviewPrompt();

    String getExplainPrompt();

    Boolean getExcludeJavaDoc();

    Boolean getUseGitIgnore();

    List<String> getExcludedDirectories();

    List<String> getIncludedFileExtensions();

    Integer getDefaultWindowContext();

    List<String> getExcludedFiles();

    void setCustomPrompts(List<CustomPrompt> customPrompts);

    void setOllamaModelUrl(String url);

    void setLmstudioModelUrl(String url);

    void setGpt4allModelUrl(String url);

    void setJanModelUrl(String url);

    void setOpenAIKey(String key);

    void setAzureOpenAIEndpoint(String endpoint);

    void setAzureOpenAIDeployment(String deployment);

    void setAzureOpenAIKey(String key);

    void setAwsAccessKeyId(String accessKeyId);
    void setAwsSecretKey(String secretKey);

    void setAwsRegion(String region);

    void setMistralKey(String key);

    void setAnthropicKey(String key);

    void setGroqKey(String key);

    void setDeepInfraKey(String key);

    void setGeminiKey(String key);

    void setDeepSeekKey(String key);

    void setOpenRouterKey(String key);

    void setIsWebSearchEnabled(Boolean flag);

    void setGoogleSearchKey(String key);

    void setGoogleCSIKey(String key);

    void setTavilySearchKey(String key);

    void setMaxSearchResults(Integer results);

    void setSelectedProvider(String projectLocation, String provider);

    void setSelectedLanguageModel(String projectLocation, String model);

    void setStreamMode(Boolean mode);

    void setTemperature(Double temperature);

    void setTopP(Double topP);

    void setTimeout(Integer timeout);

    void setMaxRetries(Integer retries);

    void setChatMemorySize(Integer size);

    void setMaxOutputTokens(Integer tokens);

    void setSystemPrompt(String prompt);

    void setExcludeJavaDoc(Boolean exclude);

    void setExcludedDirectories(List<String> directories);

    void setIncludedFileExtensions(List<String> extensions);

    String getLlamaCPPUrl();

    void setLlamaCPPUrl(String text);

    Boolean getUseFileInEditor();

    void setUseFileInEditor(Boolean useFileInEditor);

    void setUseGitIgnore(Boolean useGitIgnore);

    void setCustomOpenAIUrl(String text);

    String getCustomOpenAIUrl();

    void setCustomOpenAIModelName(String text);

    String getCustomOpenAIModelName();
}
