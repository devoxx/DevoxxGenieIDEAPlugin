package com.devoxx.genie.service.credentials;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of every secret credential stored by DevoxxGenie.
 * <p>
 * The {@link #getSubKey()} value is, by design, equal to the exact Java field name
 * on {@link com.devoxx.genie.ui.settings.DevoxxGenieStateService} that used to
 * hold the plaintext value. This 1:1 mapping is used by the credential
 * migration routine to locate the legacy field via reflection.
 */
public enum CredentialKey {
    OPEN_AI_KEY          ("openAIKey"),
    MISTRAL_KEY          ("mistralKey"),
    ANTHROPIC_KEY        ("anthropicKey"),
    GROQ_KEY             ("groqKey"),
    DEEP_INFRA_KEY       ("deepInfraKey"),
    GEMINI_KEY           ("geminiKey"),
    DEEP_SEEK_KEY        ("deepSeekKey"),
    OPEN_ROUTER_KEY      ("openRouterKey"),
    GROK_KEY             ("grokKey"),
    KIMI_KEY             ("kimiKey"),
    GLM_KEY              ("glmKey"),
    NVIDIA_KEY           ("nvidiaKey"),
    CLOUDFLARE_KEY       ("cloudflareKey"),
    AZURE_OPEN_AI_KEY    ("azureOpenAIKey"),
    AWS_ACCESS_KEY_ID    ("awsAccessKeyId"),
    AWS_SECRET_KEY       ("awsSecretKey"),
    AWS_BEARER_TOKEN     ("awsBearerToken"),
    CUSTOM_OPEN_AI_KEY   ("customOpenAIApiKey"),
    GOOGLE_SEARCH_KEY    ("googleSearchKey"),
    GOOGLE_CSI_KEY       ("googleCSIKey"),
    TAVILY_SEARCH_KEY    ("tavilySearchKey");

    /** PasswordSafe service-name prefix. Entries appear as "DevoxxGenie — &lt;subKey&gt;". */
    public static final String SERVICE_NAME = "DevoxxGenie";

    private final String subKey;

    CredentialKey(@NotNull String subKey) {
        this.subKey = subKey;
    }

    /**
     * The PasswordSafe entry sub-key. Equal to the legacy Java field name
     * on {@code DevoxxGenieStateService} so that reflection-based migration
     * can locate the source value.
     */
    public @NotNull String getSubKey() {
        return subKey;
    }

    /**
     * Build the {@link CredentialAttributes} used by IntelliJ's
     * {@code PasswordSafe} for this key.
     */
    public @NotNull CredentialAttributes attributes() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(SERVICE_NAME, subKey));
    }
}
