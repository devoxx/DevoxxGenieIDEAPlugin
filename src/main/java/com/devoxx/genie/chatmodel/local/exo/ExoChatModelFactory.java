package com.devoxx.genie.chatmodel.local.exo;

import com.devoxx.genie.chatmodel.local.LocalChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.exo.ExoModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExoChatModelFactory extends LocalChatModelFactory {

    private static final AtomicBoolean instanceReady = new AtomicBoolean(false);
    private static volatile String preparedModelId = null;
    private static final AtomicBoolean preparing = new AtomicBoolean(false);

    public ExoChatModelFactory() {
        super(ModelProvider.Exo);
    }

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        verifyAndReprepareIfNeeded(customChatModel.getModelName());
        return createOpenAiChatModel(customChatModel);
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        verifyAndReprepareIfNeeded(customChatModel.getModelName());
        return createOpenAiStreamingChatModel(customChatModel);
    }

    @Override
    protected String getModelUrl() {
        return DevoxxGenieStateService.getInstance().getExoModelUrl();
    }

    @Override
    protected ExoModelEntryDTO[] fetchModels() throws IOException {
        return ExoModelService.getInstance().getModels();
    }

    @Override
    protected LanguageModel buildLanguageModel(Object model) throws IOException {
        ExoModelEntryDTO exoModel = (ExoModelEntryDTO) model;
        int contextWindow = exoModel.getContextLength();
        if (contextWindow <= 0) {
            contextWindow = 4096;
        }
        return LanguageModel.builder()
                .provider(modelProvider)
                .modelName(exoModel.getId())
                .displayName(exoModel.getName() != null ? exoModel.getName() : exoModel.getId())
                .inputCost(0)
                .outputCost(0)
                .inputMaxTokens(contextWindow)
                .apiKeyUsed(false)
                .build();
    }

    /**
     * Quick check if the instance is still alive. If not, triggers re-preparation.
     * This is called from createChatModel on the EDT, so it must NOT block.
     */
    private void verifyAndReprepareIfNeeded(String modelId) {
        if (modelId == null) return;

        // Quick async check — don't block the EDT
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (!ExoModelService.getInstance().isInstanceRunning(modelId)) {
                instanceReady.set(false);
                prepareInstanceAsync(modelId, ProjectManager.getInstance().getDefaultProject());
            }
        });
    }

    /**
     * Prepares an Exo instance for the given model in the background.
     * Shows a progress bar and notifies when ready.
     */
    public static void prepareInstanceAsync(String modelId, Project project) {
        if (modelId == null || modelId.isBlank()) return;

        // Don't start multiple preparations
        if (!preparing.compareAndSet(false, true)) return;

        instanceReady.set(false);
        preparedModelId = modelId;

        ProgressManager.getInstance().run(new Task.Backgroundable(
                project, "Exo: Starting instance for " + modelId, true
        ) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    ExoModelService.getInstance().ensureInstanceWithProgress(modelId, indicator);
                    instanceReady.set(true);
                    NotificationUtil.sendNotification(project,
                            "Exo instance ready for " + modelId);
                } catch (IOException e) {
                    instanceReady.set(false);
                    NotificationUtil.sendNotification(project,
                            "Failed to start Exo instance: " + e.getMessage());
                } finally {
                    preparing.set(false);
                }
            }
        });
    }

    public static boolean isInstanceReady() {
        return instanceReady.get();
    }

    public static String getPreparedModelId() {
        return preparedModelId;
    }
}
