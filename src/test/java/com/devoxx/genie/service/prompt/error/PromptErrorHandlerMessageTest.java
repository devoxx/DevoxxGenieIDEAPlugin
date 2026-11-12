package com.devoxx.genie.service.prompt.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The error text shown in the chat's error card (full) and in the balloon notification (shortened). */
class PromptErrorHandlerMessageTest {

    /** A provider error longer than the 300-character summary, like Jan's "failed to open GGUF file" reply. */
    private static final String LONG_PROVIDER_ERROR = "{\"error\":{\"code\":400,\"message\":\"could not start the "
            + "llama.cpp engine: failed to load model; gguf_init_from_file: failed to open GGUF file "
            + "'/Users/someone/.cache/huggingface/hub/models--unsloth--gemma-4-E2B-it-GGUF/blobs/gemma4-e2b-it.gguf' "
            + "(No such file or directory); llama_model_load: error loading model: failed to read the file; "
            + "the final part of the message the user needs to see\"}}";

    private static final String CLOUDFLARE_2005_BODY =
            "{\"success\":false,\"result\":[],\"messages\":[],\"error\":[{\"code\":2005,"
            + "\"message\":\"Failed to get response from provider\"}],\"name\":\"AiGatewayError\","
            + "\"httpCode\":400,\"internalCode\":2005,\"message\":\"Failed to get response from provider\","
            + "\"description\":\"Failed to get response from provider\"}";

    @Test
    void fullUserFacingMessage_keepsTheWholeProviderError() {
        assertThat(LONG_PROVIDER_ERROR).hasSizeGreaterThan(300);

        String full = PromptErrorHandler.fullUserFacingMessage(new RuntimeException(LONG_PROVIDER_ERROR), null);

        assertThat(full).contains(LONG_PROVIDER_ERROR).doesNotEndWith("…");
    }

    @Test
    void userFacingMessage_stillShortensForNotifications() {
        String summary = PromptErrorHandler.userFacingMessage(new RuntimeException(LONG_PROVIDER_ERROR), null);

        assertThat(summary).hasSize(301).endsWith("…");
    }

    @Test
    void translatedProviderErrors_keepTheirWordingInBothForms() {
        RuntimeException error = new RuntimeException("Provider unavailable: " + CLOUDFLARE_2005_BODY);
        String translated = ProviderErrorTranslator.translate(error, "azure-openai/kimi-k2.6").orElseThrow();

        assertThat(PromptErrorHandler.fullUserFacingMessage(error, "azure-openai/kimi-k2.6")).isEqualTo(translated);
        assertThat(PromptErrorHandler.userFacingMessage(error, "azure-openai/kimi-k2.6")).isEqualTo(translated);
    }

    @Test
    void emptyMessage_fallsBackToTheExceptionType() {
        assertThat(PromptErrorHandler.fullUserFacingMessage(new IllegalStateException(), null)).isNotBlank();
    }
}
