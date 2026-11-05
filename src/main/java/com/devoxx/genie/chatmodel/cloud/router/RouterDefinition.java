package com.devoxx.genie.chatmodel.cloud.router;

import com.devoxx.genie.model.enumarations.ModelProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Everything the generic router provider needs to know about one OpenAI-compatible router.
 *
 * @param maxTokens fixed completion limit; null means use the value from the chat model settings
 */
public record RouterDefinition(ModelProvider provider,
                               String label,
                               String baseUrl,
                               List<String> modelEndpoints,
                               String credentialSubKey,
                               String keyUrl,
                               @Nullable String hint,
                               @Nullable CatalogFields catalogFields,
                               @Nullable Integer maxTokens) {
}
