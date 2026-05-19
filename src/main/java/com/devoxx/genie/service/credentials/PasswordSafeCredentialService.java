package com.devoxx.genie.service.credentials;

import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Default {@link CredentialService} implementation, backed by IntelliJ's
 * {@code PasswordSafe} with an in-memory fallback for environments where
 * the OS keychain is unavailable (headless tests, broken backends).
 */
public final class PasswordSafeCredentialService implements CredentialService {

    private static final Logger LOG = Logger.getInstance(PasswordSafeCredentialService.class);

    /** Used only when PasswordSafe is unavailable; otherwise serves as a write-through cache. */
    private final Map<CredentialKey, String> memoryFallback = new EnumMap<>(CredentialKey.class);

    private final boolean passwordSafeAvailable;

    public PasswordSafeCredentialService() {
        boolean available;
        try {
            available = PasswordSafe.getInstance() != null;
        } catch (Throwable t) {
            LOG.warn("PasswordSafe is not available; falling back to in-memory credential storage", t);
            available = false;
        }
        this.passwordSafeAvailable = available;
    }

    @Override
    public @NotNull String getCredential(@NotNull CredentialKey key) {
        if (!passwordSafeAvailable) {
            return memoryFallback.getOrDefault(key, "");
        }
        try {
            Credentials creds = PasswordSafe.getInstance().get(key.attributes());
            String password = (creds == null) ? null : creds.getPasswordAsString();
            if (password != null) {
                return password;
            }
        } catch (Throwable t) {
            LOG.warn("PasswordSafe.get(" + key + ") failed; using in-memory fallback", t);
        }
        return memoryFallback.getOrDefault(key, "");
    }

    @Override
    public synchronized void setCredential(@NotNull CredentialKey key, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            removeCredential(key);
            return;
        }
        if (!passwordSafeAvailable) {
            memoryFallback.put(key, value);
            return;
        }
        try {
            PasswordSafe.getInstance().set(key.attributes(), new Credentials(CredentialKey.SERVICE_NAME, value));
            // Write-through cache so reads after a failed PasswordSafe.get can still return the value
            // within the same JVM lifetime.
            memoryFallback.put(key, value);
        } catch (Throwable t) {
            LOG.warn("PasswordSafe.set(" + key + ") failed; using in-memory fallback", t);
            memoryFallback.put(key, value);
        }
    }

    @Override
    public synchronized void removeCredential(@NotNull CredentialKey key) {
        memoryFallback.remove(key);
        if (!passwordSafeAvailable) {
            return;
        }
        try {
            PasswordSafe.getInstance().set(key.attributes(), null);
        } catch (Throwable t) {
            LOG.warn("PasswordSafe.set(" + key + ", null) failed", t);
        }
    }

    @Override
    public boolean isAvailable() {
        return passwordSafeAvailable;
    }
}
