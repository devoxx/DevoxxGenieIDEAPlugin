package com.devoxx.genie.service.rag.embedding;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ProfiledEmbedderTest {

    /** Records every text it is asked to embed; returns a fixed vector. */
    private static final class RecordingModel implements EmbeddingModel {
        final List<String> seen = new ArrayList<>();
        final float[] vector;

        RecordingModel(float... vector) {
            this.vector = vector;
        }

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            List<Embedding> out = new ArrayList<>();
            for (TextSegment s : segments) {
                seen.add(s.text());
                out.add(Embedding.from(vector.clone()));
            }
            return Response.from(out);
        }
    }

    private static double norm(float[] v) {
        double sum = 0;
        for (float x : v) sum += (double) x * x;
        return Math.sqrt(sum);
    }

    // ---------------- prefixes ----------------

    @Test
    void queryPrefixIsAppliedToQueriesOnly() {
        RecordingModel model = new RecordingModel(1, 0);
        ProfiledEmbedder embedder = new ProfiledEmbedder(model,
                new EmbeddingProfile("search_query: ", "search_document: ", null));

        embedder.embedQuery("where is the indexer?");

        assertThat(model.seen).containsExactly("search_query: where is the indexer?");
    }

    @Test
    void documentPrefixIsAppliedToEveryDocumentInABatch() {
        RecordingModel model = new RecordingModel(1, 0);
        ProfiledEmbedder embedder = new ProfiledEmbedder(model,
                new EmbeddingProfile("search_query: ", "search_document: ", null));

        List<Embedding> embeddings = embedder.embedDocuments(
                List.of(TextSegment.from("class A {}"), TextSegment.from("class B {}")));

        assertThat(embeddings).hasSize(2);
        assertThat(model.seen).containsExactly("search_document: class A {}", "search_document: class B {}");
    }

    @Test
    void singleDocumentUsesTheDocumentPrefix() {
        RecordingModel model = new RecordingModel(1, 0);
        ProfiledEmbedder embedder = new ProfiledEmbedder(model, new EmbeddingProfile("q: ", "d: ", null));

        embedder.embedDocument("class A {}");

        assertThat(model.seen).containsExactly("d: class A {}");
    }

    @Test
    void prefixesNeverLeakIntoTheCallersSegments() {
        RecordingModel model = new RecordingModel(1, 0);
        ProfiledEmbedder embedder = new ProfiledEmbedder(model, new EmbeddingProfile("q: ", "d: ", null));
        TextSegment original = TextSegment.from("class A {}", Metadata.from("filePath", "/x/A.java"));

        embedder.embedDocuments(List.of(original));

        assertThat(original.text()).isEqualTo("class A {}");
        assertThat(original.metadata().getString("filePath")).isEqualTo("/x/A.java");
    }

    @Test
    void emptyProfileSendsTextUnchanged() {
        RecordingModel model = new RecordingModel(1, 0);
        ProfiledEmbedder embedder = new ProfiledEmbedder(model, EmbeddingProfile.NONE);

        embedder.embedQuery("hello");
        embedder.embedDocuments(List.of(TextSegment.from("world")));

        assertThat(model.seen).containsExactly("hello", "world");
    }

    @Test
    void nullPrefixesAreTreatedAsEmpty() {
        EmbeddingProfile profile = new EmbeddingProfile(null, null, null);
        assertThat(profile.queryPrefix()).isEmpty();
        assertThat(profile.documentPrefix()).isEmpty();
    }

    // ---------------- truncation ----------------

    @Test
    void withoutOutputDimensionTheModelVectorIsReturnedUnchanged() {
        // Not even re-normalised: the default profile must leave today's vectors bit-identical.
        ProfiledEmbedder embedder = new ProfiledEmbedder(new RecordingModel(3, 4, 12), EmbeddingProfile.NONE);

        assertThat(embedder.embedQuery("x").vector()).containsExactly(3f, 4f, 12f);
    }

    @Test
    void truncatesToTheFirstKDimensionsAndReNormalises() {
        ProfiledEmbedder embedder = new ProfiledEmbedder(new RecordingModel(3, 4, 12, 5),
                new EmbeddingProfile("", "", 2));

        float[] v = embedder.embedQuery("x").vector();

        // First two dims (3, 4) have norm 5, so they become (0.6, 0.8).
        assertThat(v).hasSize(2);
        assertThat(v[0]).isCloseTo(0.6f, within(1e-6f));
        assertThat(v[1]).isCloseTo(0.8f, within(1e-6f));
        assertThat(norm(v)).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void truncationAppliesToDocumentsToo() {
        ProfiledEmbedder embedder = new ProfiledEmbedder(new RecordingModel(3, 4, 12, 5),
                new EmbeddingProfile("", "", 2));

        List<Embedding> docs = embedder.embedDocuments(List.of(TextSegment.from("a"), TextSegment.from("b")));

        assertThat(docs).allSatisfy(e -> {
            assertThat(e.vector()).hasSize(2);
            assertThat(norm(e.vector())).isCloseTo(1.0, within(1e-6));
        });
    }

    @Test
    void outputDimensionEqualToNativeOnlyReNormalises() {
        float[] v = ProfiledEmbedder.truncateAndNormalize(new float[]{3, 4}, 2);
        assertThat(v[0]).isCloseTo(0.6f, within(1e-6f));
        assertThat(v[1]).isCloseTo(0.8f, within(1e-6f));
    }

    @Test
    void outputDimensionLargerThanNativeIsAnError() {
        ProfiledEmbedder embedder = new ProfiledEmbedder(new RecordingModel(1, 0, 0),
                new EmbeddingProfile("", "", 768));

        assertThatThrownBy(() -> embedder.embedQuery("x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("768")
                .hasMessageContaining("3");
    }

    @Test
    void allZeroVectorIsNotTurnedIntoNaN() {
        float[] v = ProfiledEmbedder.truncateAndNormalize(new float[]{0, 0, 0}, 2);
        assertThat(v).containsExactly(0f, 0f);
    }

    @Test
    void nonPositiveOutputDimensionIsRejected() {
        assertThatThrownBy(() -> new EmbeddingProfile("", "", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmbeddingProfile("", "", -5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void truncationDoesNotMutateTheInputArray() {
        float[] input = {3, 4, 12};
        ProfiledEmbedder.truncateAndNormalize(input, 2);
        assertThat(input).containsExactly(3f, 4f, 12f);
    }
}
