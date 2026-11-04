package com.devoxx.genie.service.rag.embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TASK-265: the fingerprint's extended fields, prefix defaults, providers and the client factory. */
class EmbeddingSetupTest {

    private static final String NOMIC = "nomic-embed-text";

    @Test
    void idsWrittenBeforeThisTaskStillParse() {
        assertThat(EmbeddingFingerprint.parse("ollama:nomic-embed-text")).isEqualTo(EmbeddingFingerprint.LEGACY);
        assertThat(EmbeddingFingerprint.parse("ollama:nomic-embed-text:v1.5").modelName())
                .isEqualTo("nomic-embed-text:v1.5");
        assertThat(EmbeddingFingerprint.parse(null)).isEqualTo(EmbeddingFingerprint.LEGACY);
    }

    @Test
    void everyFieldSurvivesTheRoundTrip() {
        EmbeddingFingerprint fingerprint = new EmbeddingFingerprint(
                "custom-openai", "hf.co/org/model|odd:Q8_0%", 256, "search_document: a|b=c ");

        assertThat(EmbeddingFingerprint.parse(fingerprint.id())).isEqualTo(fingerprint);
    }

    @Test
    void eachFieldThatChangesTheVectorsChangesTheFingerprint() {
        EmbeddingFingerprint base = new EmbeddingFingerprint("ollama", NOMIC, 512, "doc: ");

        assertThat(new EmbeddingFingerprint("openai", NOMIC, 512, "doc: ").id()).isNotEqualTo(base.id());
        assertThat(new EmbeddingFingerprint("ollama", "bge-m3", 512, "doc: ").id()).isNotEqualTo(base.id());
        assertThat(new EmbeddingFingerprint("ollama", NOMIC, 256, "doc: ").id()).isNotEqualTo(base.id());
        assertThat(new EmbeddingFingerprint("ollama", NOMIC, 512, "other: ").id()).isNotEqualTo(base.id());
    }

    @Test
    void upgradedInstallsAreStaleBecauseTheNewDefaultAddsTheDocumentPrefix() {
        EmbeddingFingerprint newDefault = new EmbeddingFingerprint("ollama", NOMIC, null,
                EmbeddingPrefixes.defaultDocumentPrefix(NOMIC));

        assertThat(newDefault).isNotEqualTo(EmbeddingFingerprint.LEGACY);
    }

    @Test
    void latestTagIsOnlyDroppedForOllama() {
        assertThat(new EmbeddingFingerprint("ollama", "bge-m3:latest").modelName()).isEqualTo("bge-m3");
        assertThat(new EmbeddingFingerprint("openai", "bge-m3:latest").modelName()).isEqualTo("bge-m3:latest");
    }

    @Test
    void profilesCarryThePrefixForTheirOwnPathOnly() {
        EmbeddingFingerprint fingerprint = new EmbeddingFingerprint("ollama", NOMIC, 256, "doc: ");

        assertThat(fingerprint.documentProfile()).isEqualTo(new EmbeddingProfile("", "doc: ", 256));
        assertThat(fingerprint.queryProfile("query: ")).isEqualTo(new EmbeddingProfile("query: ", "", 256));
    }

    @Test
    void nomicGetsItsDocumentedPrefixesAndOtherModelsNone() {
        assertThat(EmbeddingPrefixes.defaultQueryPrefix(NOMIC)).isEqualTo("search_query: ");
        assertThat(EmbeddingPrefixes.defaultDocumentPrefix("nomic-embed-text:v1.5")).isEqualTo("search_document: ");
        assertThat(EmbeddingPrefixes.defaultQueryPrefix("bge-m3")).isEmpty();
        assertThat(EmbeddingPrefixes.defaultDocumentPrefix(null)).isEmpty();
    }

    @Test
    void storedNullFollowsTheModelButAnEmptyPrefixIsKept() {
        assertThat(EmbeddingPrefixes.resolve(null, "search_query: ")).isEqualTo("search_query: ");
        assertThat(EmbeddingPrefixes.resolve("", "search_query: ")).isEmpty();
        assertThat(EmbeddingPrefixes.toStored("search_query: ", "search_query: ")).isNull();
        assertThat(EmbeddingPrefixes.toStored("", "search_query: ")).isEmpty();
    }

    @Test
    void prefixFieldFollowsTheModelUnlessTheUserTypedTheirOwn() {
        assertThat(EmbeddingPrefixes.afterModelChange("search_query: ", "search_query: ", "")).isEmpty();
        assertThat(EmbeddingPrefixes.afterModelChange("", "", "search_query: ")).isEqualTo("search_query: ");
        assertThat(EmbeddingPrefixes.afterModelChange("mine: ", "search_query: ", "")).isEqualTo("mine: ");
    }

    @Test
    void unknownProviderIdFallsBackToOllama() {
        assertThat(EmbeddingProvider.fromId(null)).isEqualTo(EmbeddingProvider.OLLAMA);
        assertThat(EmbeddingProvider.fromId("gone")).isEqualTo(EmbeddingProvider.OLLAMA);
        assertThat(EmbeddingProvider.fromId("lmstudio")).isEqualTo(EmbeddingProvider.LM_STUDIO);
    }

    @Test
    void warningAppliesToOpenAiAndToAnythingBeyondThisMachine() {
        assertThat(EmbeddingProvider.OPENAI.sendsCodeOffMachine(null)).isTrue();
        assertThat(EmbeddingProvider.CUSTOM_OPENAI.sendsCodeOffMachine("https://api.example.com/v1")).isTrue();
        assertThat(EmbeddingProvider.LLAMA_CPP.sendsCodeOffMachine("http://192.168.1.5:8080")).isTrue();
        assertThat(EmbeddingProvider.OLLAMA.sendsCodeOffMachine("http://localhost:11434/")).isFalse();
        assertThat(EmbeddingProvider.LM_STUDIO.sendsCodeOffMachine("http://127.0.0.1:1234/v1/")).isFalse();
        assertThat(EmbeddingProvider.CUSTOM_OPENAI.sendsCodeOffMachine("")).isFalse();
    }

    @Test
    void llamaCppGetsV1AndOtherProvidersKeepTheirUrl() {
        assertThat(EmbeddingModelFactory.openAiBaseUrl(EmbeddingProvider.LLAMA_CPP, "http://localhost:8080"))
                .isEqualTo("http://localhost:8080/v1");
        assertThat(EmbeddingModelFactory.openAiBaseUrl(EmbeddingProvider.LLAMA_CPP, "http://localhost:8080/v1/"))
                .isEqualTo("http://localhost:8080/v1");
        assertThat(EmbeddingModelFactory.openAiBaseUrl(EmbeddingProvider.CUSTOM_OPENAI, "http://host/api/"))
                .isEqualTo("http://host/api");
    }

    @Test
    void openAiStyleProviderCallsTheEmbeddingsEndpointWithoutADimension() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(embeddingResponse());
            server.start();
            String url = server.url("/v1").toString();

            EmbeddingModel model = EmbeddingModelFactory.create(EmbeddingProvider.CUSTOM_OPENAI, url, "secret", "my-model");
            float[] vector = model.embed("hello").content().vector();

            RecordedRequest request = server.takeRequest();
            assertThat(request.getPath()).isEqualTo("/v1/embeddings");
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer secret");
            assertThat(request.getBody().readUtf8()).contains("my-model").doesNotContain("dimensions");
            assertThat(vector).containsExactly(0.6f, 0.8f);
        }
    }

    @Test
    void localServerWithoutAKeyStillWorks() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(embeddingResponse());
            server.start();

            EmbeddingModelFactory.create(EmbeddingProvider.LM_STUDIO, server.url("/v1/").toString(), null, "m")
                    .embed("hello");

            assertThat(server.takeRequest().getPath()).isEqualTo("/v1/embeddings");
        }
    }

    private static MockResponse embeddingResponse() {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody("""
                {"object":"list","model":"m","usage":{"prompt_tokens":1,"total_tokens":1},
                 "data":[{"object":"embedding","index":0,"embedding":[0.6,0.8]}]}
                """);
    }
}
