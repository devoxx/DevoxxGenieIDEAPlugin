package com.devoxx.genie.service.trace.hf;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class HfDatasetTargetTest {

    @ParameterizedTest
    @ValueSource(strings = {"alice/traces", "datasets/alice/traces", "hf://datasets/alice/traces",
            "https://huggingface.co/datasets/alice/traces", "https://huggingface.co/datasets/alice/traces/",
            "  alice/traces  "})
    void acceptedForms_parseToTheSameDataset(String input) {
        HfDatasetTarget target = HfDatasetTarget.parse(input);
        assertThat(target).isNotNull();
        assertThat(target.repoId()).isEqualTo("alice/traces");
        assertThat(target.toString()).isEqualTo("datasets/alice/traces");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "traces", "a/b/c", "buckets/alice/traces", "alice/tr aces", "../x"})
    void invalidOrUnsupported_returnsNull(String input) {
        assertThat(HfDatasetTarget.parse(input)).isNull();
    }

    @org.junit.jupiter.api.Test
    void urls() {
        HfDatasetTarget t = new HfDatasetTarget("alice", "traces");
        assertThat(t.browserUrl("https://huggingface.co/")).isEqualTo("https://huggingface.co/datasets/alice/traces");
        assertThat(t.fileUrl("https://huggingface.co", "devoxxgenie/1.jsonl"))
                .isEqualTo("https://huggingface.co/datasets/alice/traces/blob/main/devoxxgenie/1.jsonl");
    }
}
