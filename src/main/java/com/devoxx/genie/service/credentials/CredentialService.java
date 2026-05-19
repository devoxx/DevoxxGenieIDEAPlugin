package com.devoxx.genie.service.credentials;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Application-scoped service that stores DevoxxGenie API keys and tokens in
 * IntelliJ's {@code PasswordSafe} (OS keychain / KeePass / KWallet, depending
 * on the platform).
 * <p>
 * Implementations must degrade gracefully — see {@link #isAvailable()} —
 * because PasswordSafe can be missing or broken in headless test runs and
 * some container/cloud IDE environments.
 */
public interface CredentialService {

    static @NotNull CredentialService getInstance() {
        return ApplicationManager.getApplication().getService(CredentialService.class);
    }

    /**
     * Returns the credential value for {@code key}, or {@code ""} if absent
     * or if PasswordSafe is unavailable. Never returns {@code null}.
     */
    @NotNull String getCredential(@NotNull CredentialKey key);

    /**
     * Stores {@code value} under {@code key}. A {@code null} or empty value
     * removes the entry, equivalent to {@link #removeCredential(CredentialKey)}.
     */
    void setCredential(@NotNull CredentialKey key, @Nullable String value);

    /** Removes the credential entry for {@code key}. */
    void removeCredential(@NotNull CredentialKey key);

    /**
     * {@code true} if IntelliJ's {@code PasswordSafe} is reachable for this
     * process. When {@code false}, get/set operate on an in-memory fallback
     * map and values are lost on JVM restart.
     */
    boolean isAvailable();
}
