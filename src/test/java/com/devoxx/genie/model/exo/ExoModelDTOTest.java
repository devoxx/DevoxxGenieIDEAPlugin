package com.devoxx.genie.model.exo;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExoModelDTOTest {

    private static final Gson gson = new Gson();

    private static final String FULL_RESPONSE = """
            {
                "object": "list",
                "data": [
                    {
                        "id": "mlx-community/Llama-3.2-1B-Instruct-4bit",
                        "name": "Llama-3.2-1B-Instruct-4bit",
                        "context_length": 131072,
                        "storage_size_megabytes": 696,
                        "supports_tensor": true,
                        "family": "llama",
                        "quantization": "4bit",
                        "base_model": "Llama 3.2 1B Instruct",
                        "capabilities": ["text"]
                    },
                    {
                        "id": "mlx-community/MiniMax-M2.5-6bit",
                        "name": "MiniMax-M2.5-6bit",
                        "context_length": 196608,
                        "storage_size_megabytes": 173000,
                        "supports_tensor": true,
                        "family": "minimax",
                        "quantization": "6bit",
                        "base_model": "MiniMax M2.5",
                        "capabilities": ["text", "thinking"]
                    }
                ]
            }
            """;

    @Test
    void shouldDeserializeFullResponse() {
        ExoModelDTO dto = gson.fromJson(FULL_RESPONSE, ExoModelDTO.class);

        assertThat(dto.getObject()).isEqualTo("list");
        assertThat(dto.getData()).hasSize(2);
    }

    @Test
    void shouldDeserializeModelEntry() {
        ExoModelDTO dto = gson.fromJson(FULL_RESPONSE, ExoModelDTO.class);
        ExoModelEntryDTO first = dto.getData()[0];

        assertThat(first.getId()).isEqualTo("mlx-community/Llama-3.2-1B-Instruct-4bit");
        assertThat(first.getName()).isEqualTo("Llama-3.2-1B-Instruct-4bit");
        assertThat(first.getContextLength()).isEqualTo(131072);
        assertThat(first.getStorageSizeMegabytes()).isEqualTo(696);
        assertThat(first.isSupportsTensor()).isTrue();
        assertThat(first.getFamily()).isEqualTo("llama");
        assertThat(first.getQuantization()).isEqualTo("4bit");
        assertThat(first.getBaseModel()).isEqualTo("Llama 3.2 1B Instruct");
        assertThat(first.getCapabilities()).containsExactly("text");
    }

    @Test
    void shouldDeserializeModelWithMultipleCapabilities() {
        ExoModelDTO dto = gson.fromJson(FULL_RESPONSE, ExoModelDTO.class);
        ExoModelEntryDTO second = dto.getData()[1];

        assertThat(second.getId()).isEqualTo("mlx-community/MiniMax-M2.5-6bit");
        assertThat(second.getContextLength()).isEqualTo(196608);
        assertThat(second.getCapabilities()).containsExactly("text", "thinking");
    }

    @Test
    void shouldHandleEmptyDataArray() {
        String json = """
                {
                    "object": "list",
                    "data": []
                }
                """;

        ExoModelDTO dto = gson.fromJson(json, ExoModelDTO.class);

        assertThat(dto.getData()).isEmpty();
    }
}
