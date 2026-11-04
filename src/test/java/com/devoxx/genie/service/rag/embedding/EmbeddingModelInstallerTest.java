package com.devoxx.genie.service.rag.embedding;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingModelInstallerTest {

    private final List<String> askedAbout = new ArrayList<>();
    private final List<String> statuses = new ArrayList<>();

    @Test
    void installedModelNeedsNoQuestionAndNoDownload() {
        FakeOllama ollama = new FakeOllama("nomic-embed-text:latest");

        EmbeddingModelInstaller.Result result = ensure("nomic-embed-text", ollama, true);

        assertThat(result.ready()).isTrue();
        assertThat(askedAbout).isEmpty();
        assertThat(ollama.pulled).isEmpty();
    }

    @Test
    void missingModelIsDownloadedOnceTheUserAgrees() {
        FakeOllama ollama = new FakeOllama("nomic-embed-text:latest");

        EmbeddingModelInstaller.Result result = ensure("embeddinggemma", ollama, true);

        assertThat(result.ready()).isTrue();
        assertThat(askedAbout).containsExactly("embeddinggemma");
        assertThat(ollama.pulled).containsExactly("embeddinggemma");
        assertThat(statuses).contains("pulling manifest");
    }

    @Test
    void missingModelIsNotDownloadedWhenTheUserDeclines() {
        FakeOllama ollama = new FakeOllama();

        EmbeddingModelInstaller.Result result = ensure("embeddinggemma", ollama, false);

        assertThat(result.ready()).isFalse();
        assertThat(result.problem()).contains("embeddinggemma").contains("not changed");
        assertThat(ollama.pulled).isEmpty();
    }

    @Test
    void failedDownloadIsReportedAsNotReady() {
        FakeOllama ollama = new FakeOllama();
        ollama.pullFailure = new IOException("pull model manifest: file does not exist");

        EmbeddingModelInstaller.Result result = ensure("typo-model", ollama, true);

        assertThat(result.ready()).isFalse();
        assertThat(result.problem()).contains("typo-model").contains("file does not exist").contains("not changed");
    }

    @Test
    void downloadThatDoesNotProduceTheModelIsNotReady() {
        FakeOllama ollama = new FakeOllama();
        ollama.pullInstalls = false;

        EmbeddingModelInstaller.Result result = ensure("embeddinggemma", ollama, true);

        assertThat(result.ready()).isFalse();
        assertThat(result.problem()).contains("embeddinggemma");
    }

    @Test
    void unreachableOllamaStopsWithoutAskingToDownload() {
        FakeOllama ollama = new FakeOllama();
        ollama.listFailure = new IOException("Connection refused");

        EmbeddingModelInstaller.Result result = ensure("embeddinggemma", ollama, true);

        assertThat(result.ready()).isFalse();
        assertThat(result.problem()).contains("Ollama").contains("not changed");
        assertThat(askedAbout).isEmpty();
    }

    private EmbeddingModelInstaller.Result ensure(String model, FakeOllama ollama, boolean userAgrees) {
        return EmbeddingModelInstaller.ensureInstalled(model, ollama, name -> {
            askedAbout.add(name);
            return userAgrees;
        }, statuses::add);
    }

    private static final class FakeOllama implements EmbeddingModelInstaller.Ollama {
        private final List<String> installed;
        private final List<String> pulled = new ArrayList<>();
        private IOException listFailure;
        private IOException pullFailure;
        private boolean pullInstalls = true;

        private FakeOllama(String... installed) {
            this.installed = new ArrayList<>(List.of(installed));
        }

        @Override
        public List<String> installedModels() throws IOException {
            if (listFailure != null) throw listFailure;
            return installed;
        }

        @Override
        public void pull(String model, Consumer<String> status) throws IOException {
            if (pullFailure != null) throw pullFailure;
            pulled.add(model);
            status.accept("pulling manifest");
            if (pullInstalls) installed.add(model + ":latest");
        }
    }
}
