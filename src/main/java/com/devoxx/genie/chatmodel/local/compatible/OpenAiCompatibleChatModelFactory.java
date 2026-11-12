package com.devoxx.genie.chatmodel.local.compatible;

import com.devoxx.genie.chatmodel.catalog.CatalogDefaults;
import com.devoxx.genie.chatmodel.catalog.ModelCatalogParser;
import com.devoxx.genie.chatmodel.local.LocalChatModelFactory;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.google.gson.JsonParseException;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * One factory for every local OpenAI-compatible provider described by a {@link LocalProviderDefinition}.
 * It lists models from {@code <url>models} and builds chat clients from the shared OpenAI classes.
 */
public class OpenAiCompatibleChatModelFactory extends LocalChatModelFactory {

    /** Fetches the body of the models endpoint; replaceable in tests. */
    @FunctionalInterface
    interface CatalogBodyFetcher {
        String fetch(String url) throws IOException;
    }

    private final LocalProviderDefinition definition;
    private final BooleanSupplier enabled;
    private final Supplier<String> url;
    private final Supplier<Integer> fallbackContext;
    private final CatalogBodyFetcher fetcher;

    public OpenAiCompatibleChatModelFactory(@NotNull LocalProviderDefinition definition) {
        this(definition,
                () -> DevoxxGenieStateService.getInstance().isLocalProviderEnabled(definition.provider()),
                () -> DevoxxGenieStateService.getInstance().getLocalProviderUrl(definition.provider()),
                () -> DevoxxGenieStateService.getInstance().getLocalProviderFallbackContext(definition.provider()),
                LocalLLMProviderUtil::getBodyFromUrl);
    }

    OpenAiCompatibleChatModelFactory(@NotNull LocalProviderDefinition definition,
                                     @NotNull BooleanSupplier enabled,
                                     @NotNull Supplier<String> url,
                                     @NotNull Supplier<Integer> fallbackContext,
                                     @NotNull CatalogBodyFetcher fetcher) {
        super(definition.provider());
        this.definition = definition;
        this.enabled = enabled;
        this.url = url;
        this.fallbackContext = fallbackContext;
        this.fetcher = fetcher;
    }

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        return createOpenAiChatModel(customChatModel);
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        return createOpenAiStreamingChatModel(customChatModel);
    }

    @Override
    protected dev.langchain4j.http.client.HttpClientBuilder resolveHttpClientBuilder() {
        dev.langchain4j.http.client.HttpClientBuilder builder = super.resolveHttpClientBuilder();
        return definition.compactJsonRequestBodies() ? new CompactJsonHttpClientBuilder(builder) : builder;
    }

    @Override
    protected String getModelUrl() {
        return url.get();
    }

    @Override
    public List<LanguageModel> getModels() {
        return enabled.getAsBoolean() ? super.getModels() : List.of();
    }

    @Override
    protected LanguageModel[] fetchModels() throws IOException {
        String base = url.get().trim();
        String modelsUrl = base.endsWith("/") ? base + "models" : base + "/models";
        Integer fallback = fallbackContext.get();
        CatalogDefaults defaults = new CatalogDefaults(
                fallback != null ? fallback : definition.defaultContextLength(), false, true);
        try {
            return ModelCatalogParser.parse(definition.provider(), definition.catalogFields(), defaults,
                    fetcher.fetch(modelsUrl)).toArray(new LanguageModel[0]);
        } catch (JsonParseException e) {
            // LocalChatModelFactory only treats IOException as "provider not running"
            throw new IOException("Unexpected reply from " + modelsUrl, e);
        }
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) {
        return (LanguageModel) model;
    }

    @Override
    protected void handleProviderNotRunning() {
        NotificationUtil.sendNotification(ProjectManager.getInstance().getDefaultProject(),
                definition.provider().getName() + " is not running. Please start it and try again.");
    }
}
