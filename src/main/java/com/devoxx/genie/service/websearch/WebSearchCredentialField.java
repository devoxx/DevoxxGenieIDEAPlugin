package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.credentials.CredentialKey;
import org.jetbrains.annotations.NotNull;

/**
 * One credential a web search provider needs, shown as a password field in Settings → Web search.
 *
 * @param key      where the value is stored (PasswordSafe)
 * @param label    the settings label, also used in "missing ..." messages (e.g. "API key")
 * @param helpUrl  where the user obtains the value
 * @param required whether the provider is unusable without it
 */
public record WebSearchCredentialField(@NotNull CredentialKey key,
                                       @NotNull String label,
                                       @NotNull String helpUrl,
                                       boolean required) {
}
