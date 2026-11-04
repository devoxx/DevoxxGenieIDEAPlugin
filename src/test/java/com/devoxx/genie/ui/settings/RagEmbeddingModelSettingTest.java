package com.devoxx.genie.ui.settings;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TASK-212: the RAG embedding model setting and how it persists. */
class RagEmbeddingModelSettingTest {

    @Test
    void defaultIsNomicEmbedTextSoExistingInstallsSeeNoChange() {
        assertThat(new DevoxxGenieStateService().getRagEmbeddingModelName()).isEqualTo("nomic-embed-text");
    }

    @Test
    void configuredModelSurvivesTheXmlRoundTrip() throws Exception {
        DevoxxGenieStateService source = new DevoxxGenieStateService();
        source.setRagEmbeddingModelName("bge-m3");

        Element xml = XmlSerializer.serialize(source);
        DevoxxGenieStateService restored = XmlSerializer.deserialize(
                JDOMUtil.load(JDOMUtil.writeElement(xml)), DevoxxGenieStateService.class);

        assertThat(restored.getRagEmbeddingModelName()).isEqualTo("bge-m3");
    }

    @Test
    void embeddingSettingsSurviveTheXmlRoundTripIncludingTrailingSpaceAndEmptyPrefix() throws Exception {
        DevoxxGenieStateService source = new DevoxxGenieStateService();
        source.setRagEmbeddingProvider("llamacpp");
        source.setRagQueryPrefix("Query: ");
        source.setRagDocumentPrefix("");
        source.setRagOutputDimension(256);

        DevoxxGenieStateService restored = XmlSerializer.deserialize(
                JDOMUtil.load(JDOMUtil.writeElement(XmlSerializer.serialize(source))), DevoxxGenieStateService.class);

        assertThat(restored.getRagEmbeddingProvider()).isEqualTo("llamacpp");
        assertThat(restored.getRagQueryPrefix()).isEqualTo("Query: ");
        assertThat(restored.getRagDocumentPrefix()).as("empty means no prefix, not the model's default").isEmpty();
        assertThat(restored.getRagOutputDimension()).isEqualTo(256);
    }

    @Test
    void settingsSavedBeforeProvidersExistedUseOllamaAndFollowTheModelsPrefixes() throws Exception {
        Element legacy = JDOMUtil.load("""
                <DevoxxGenieStateService>
                  <option name="ragEnabled" value="true" />
                </DevoxxGenieStateService>
                """);

        DevoxxGenieStateService restored = XmlSerializer.deserialize(legacy, DevoxxGenieStateService.class);

        assertThat(restored.getRagEmbeddingProvider()).isEqualTo("ollama");
        assertThat(restored.getRagQueryPrefix()).isNull();
        assertThat(restored.getRagDocumentPrefix()).isNull();
        assertThat(restored.getRagOutputDimension()).isNull();
    }

    @Test
    void settingsSavedBeforeTheOptionExistedLoadTheDefault() throws Exception {
        // A pre-TASK-212 settings file has no ragEmbeddingModelName option at all.
        Element legacy = JDOMUtil.load("""
                <DevoxxGenieStateService>
                  <option name="ragEnabled" value="true" />
                </DevoxxGenieStateService>
                """);

        DevoxxGenieStateService restored = XmlSerializer.deserialize(legacy, DevoxxGenieStateService.class);

        assertThat(restored.getRagEnabled()).isTrue();
        assertThat(restored.getRagEmbeddingModelName()).isEqualTo("nomic-embed-text");
    }
}
