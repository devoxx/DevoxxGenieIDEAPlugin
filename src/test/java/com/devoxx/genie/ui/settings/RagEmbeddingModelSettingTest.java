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
