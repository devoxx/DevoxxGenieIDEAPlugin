package com.devoxx.genie.model.lmstudio;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LMStudioModelEntryDTOTest {

    private final Gson gson = new Gson();

    @Test
    void resolveContextLength_usesMaxContextLengthWhenPresent() {
        LMStudioModelEntryDTO model = gson.fromJson("""
                {
                  "id": "mistralai/devstral-small-2-2512",
                  "max_context_length": 393216
                }
                """, LMStudioModelEntryDTO.class);

        assertThat(model.resolveContextLength()).isEqualTo(393216);
    }

    @Test
    void resolveContextLength_usesLoadedInstanceContextLengthAsFallback() {
        LMStudioModelEntryDTO model = gson.fromJson("""
                {
                  "id": "mistralai/devstral-small-2-2512",
                  "loaded_instances": [
                    {
                      "id": "mistralai/devstral-small-2-2512",
                      "config": {
                        "context_length": 393216
                      }
                    }
                  ]
                }
                """, LMStudioModelEntryDTO.class);

        assertThat(model.resolveContextLength()).isEqualTo(393216);
    }

    @Test
    void resolveContextLength_supportsCamelCaseAlternativeField() {
        LMStudioModelEntryDTO model = gson.fromJson("""
                {
                  "id": "example/model",
                  "maxContextLength": 131072
                }
                """, LMStudioModelEntryDTO.class);

        assertThat(model.resolveContextLength()).isEqualTo(131072);
    }

    @Test
    void resolveContextLengthOrDefault_returnsDefaultWhenNoContextLengthFound() {
        LMStudioModelEntryDTO model = gson.fromJson("""
                {
                  "id": "example/model"
                }
                """, LMStudioModelEntryDTO.class);

        assertThat(model.resolveContextLengthOrDefault(8000)).isEqualTo(8000);
    }

    @Test
    void resolveModelName_supportsApiV1ModelsKeyField() {
        LMStudioModelEntryDTO model = gson.fromJson("""
                {
                  "key": "text-embedding-nomic-embed-text-v1.5"
                }
                """, LMStudioModelEntryDTO.class);

        assertThat(model.resolveModelName()).isEqualTo("text-embedding-nomic-embed-text-v1.5");
    }

    @Test
    void resolveDisplayName_prefersDisplayNameFromApiV1Models() {
        LMStudioModelEntryDTO model = gson.fromJson("""
                {
                  "key": "text-embedding-nomic-embed-text-v1.5",
                  "display_name": "Nomic Embed Text v1.5"
                }
                """, LMStudioModelEntryDTO.class);

        assertThat(model.resolveDisplayName()).isEqualTo("Nomic Embed Text v1.5");
    }

    /**
     * Regression test for <a href="https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/809">#809</a>:
     * Parses the exact JSON from the issue to verify context length is not stuck at 8K default.
     */
    @Test
    void issue809_fullApiV1ModelsResponse_parsesContextLengthCorrectly() {
        LMStudioModelEntryDTO model = gson.fromJson("""
                {
                  "type": "llm",
                  "publisher": "mistralai",
                  "key": "mistralai/devstral-small-2-2512",
                  "display_name": "Devstral Small 2 2512",
                  "architecture": "mistral3",
                  "quantization": {
                    "name": "4bit",
                    "bits_per_weight": 4
                  },
                  "size_bytes": 14120998772,
                  "params_string": "24B",
                  "loaded_instances": [
                    {
                      "id": "mistralai/devstral-small-2-2512",
                      "config": {
                        "context_length": 393216
                      }
                    }
                  ],
                  "max_context_length": 393216,
                  "format": "mlx",
                  "capabilities": {
                    "vision": true,
                    "trained_for_tool_use": true
                  },
                  "description": null,
                  "variants": [
                    "mistralai/devstral-small-2-2512@4bit"
                  ],
                  "selected_variant": "mistralai/devstral-small-2-2512@4bit"
                }
                """, LMStudioModelEntryDTO.class);

        // Issue #809: context length should NOT default to 8000
        assertThat(model.resolveModelName()).isEqualTo("mistralai/devstral-small-2-2512");
        assertThat(model.resolveDisplayName()).isEqualTo("Devstral Small 2 2512");
        assertThat(model.resolveContextLength()).isEqualTo(393216);
        assertThat(model.resolveContextLengthOrDefault(8000)).isEqualTo(393216);
    }
}
