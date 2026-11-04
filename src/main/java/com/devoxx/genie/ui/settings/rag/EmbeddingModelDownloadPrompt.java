package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.chatmodel.local.ollama.OllamaModelService;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.service.rag.embedding.EmbeddingModelInstaller;
import com.devoxx.genie.service.rag.embedding.EmbeddingProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Connects {@link EmbeddingModelInstaller} to the real Ollama service and a Yes/No dialog. */
final class EmbeddingModelDownloadPrompt {

    private EmbeddingModelDownloadPrompt() {}

    /**
     * Blocks until the configured embedding model is installed or the user declines, so call
     * it off the EDT.
     *
     * @param parent component the dialog belongs to, needed above the modal Settings dialog;
     *               {@code null} uses the project window
     */
    static @NotNull EmbeddingModelInstaller.Result ensureInstalled(@NotNull Project project,
                                                                   @Nullable Component parent,
                                                                   @NotNull Consumer<String> status) {
        EmbeddingFingerprint configured = ChromaEmbeddingService.getInstance().configuredFingerprint();
        if (!EmbeddingFingerprint.OLLAMA.equals(configured.provider())) {
            String savedModel = DevoxxGenieStateService.getInstance().getRagEmbeddingModelName();
            if (savedModel == null || savedModel.isBlank()) {
                return EmbeddingModelInstaller.Result.failure(missingModelMessage(
                        EmbeddingProvider.fromId(configured.provider())));
            }
            // No download API yet (TASK-271). The indexer's test embed reports a missing model.
            return EmbeddingModelInstaller.Result.success();
        }
        String model = configured.modelName();
        return EmbeddingModelInstaller.ensureInstalled(model, new InstalledOllama(),
                name -> askToDownload(project, parent, name), status);
    }

    /** Only Ollama has a default model, so other providers must be told which model to use. */
    static @NotNull String missingModelMessage(@NotNull EmbeddingProvider provider) {
        return "Enter an embedding model for " + provider + " in the RAG settings. The existing RAG index was not changed.";
    }

    static @NotNull String question(@NotNull String model) {
        return "Embedding model " + model + " isn't installed in Ollama. Download it now?\n\n"
                + "This can take several minutes. Indexing starts when the download finishes.";
    }

    private static boolean askToDownload(Project project, @Nullable Component parent, String model) {
        AtomicBoolean agreed = new AtomicBoolean();
        // ModalityState.any(): the Settings dialog is modal, and the question must show above it.
        ApplicationManager.getApplication().invokeAndWait(() -> {
            MessageDialogBuilder.YesNo dialog = MessageDialogBuilder.yesNo("Download Embedding Model", question(model))
                    .yesText("Download")
                    .noText("Cancel");
            agreed.set(parent != null ? dialog.ask(parent) : dialog.ask(project));
        }, ModalityState.any());
        return agreed.get();
    }

    private static final class InstalledOllama implements EmbeddingModelInstaller.ModelSource {
        @Override
        public List<String> installedModels() throws IOException {
            OllamaModelEntryDTO[] models;
            try {
                models = OllamaModelService.getInstance().getModels();
            } catch (ProcessCanceledException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
            if (models == null) {
                throw new IOException("Ollama returned no model list");
            }
            return Arrays.stream(models)
                    .map(OllamaModelEntryDTO::getName)
                    .filter(name -> name != null)
                    .toList();
        }

        @Override
        public void pull(String model, Consumer<String> status) throws IOException {
            OllamaModelService.getInstance().pullModel(model, status);
        }
    }
}
