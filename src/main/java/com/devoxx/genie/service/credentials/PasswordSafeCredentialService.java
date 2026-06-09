package com.devoxx.genie.service.credentials;

import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;

/**
 * Default {@link CredentialService} implementation, backed by IntelliJ's
 * {@code PasswordSafe} with an in-memory fallback for environments where
 * the OS keychain is unavailable (headless tests, broken backends).
 */
public final class PasswordSafeCredentialService implements CredentialService {

    private static final Logger LOG = Logger.getInstance(PasswordSafeCredentialService.class);

    /**
     * Fallback store used when PasswordSafe is unavailable. It also holds a write-side cache
     * of credentials set during this JVM lifetime (populated by {@link #setCredential}, not by
     * reads), so a value remains readable even if a later PasswordSafe {@code get()} fails.
     */
    private final Map<CredentialKey, String> memoryFallback = new EnumMap<>(CredentialKey.class);

    /** Keys whose PasswordSafe read returned null/empty or threw — skip the keychain on repeat reads. */
    private final Set<CredentialKey> emptyKeys = EnumSet.noneOf(CredentialKey.class);

    private final boolean passwordSafeAvailable;

    public PasswordSafeCredentialService() {
        boolean available;
        try {
            // Skip PasswordSafe in headless mode (e.g. buildSearchableOptions, tests) to
            // avoid triggering repeated OS keychain dialogs that can never be confirmed.
            available = !isHeadless() && PasswordSafe.getInstance() != null;
        } catch (Throwable t) {
            LOG.warn("PasswordSafe is not available; falling back to in-memory credential storage", t);
            available = false;
        }
        this.passwordSafeAvailable = available;
    }

    private static boolean isHeadless() {
        try {
            return ApplicationManager.getApplication().isHeadlessEnvironment();
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public synchronized @NotNull String getCredential(@NotNull CredentialKey key) {
        if (!passwordSafeAvailable) {
            return memoryFallback.getOrDefault(key, "");
        }
        // Skip keychain if a prior read returned nothing — avoids repeated OS dialogs per session.
        if (emptyKeys.contains(key)) {
            return memoryFallback.getOrDefault(key, "");
        }
        try {
            Credentials creds = PasswordSafe.getInstance().get(key.attributes());
            String password = (creds == null) ? null : creds.getPasswordAsString();
            if (password != null && !password.isEmpty()) {
                return password;
            }
            emptyKeys.add(key);
        } catch (Throwable t) {
            LOG.warn("PasswordSafe.get(" + key + ") failed; using in-memory fallback", t);
            emptyKeys.add(key);
        }
        return memoryFallback.getOrDefault(key, "");
    }

    @Override
    public synchronized void setCredential(@NotNull CredentialKey key, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            removeCredential(key);
            return;
        }
        // A new value was written — allow the next read to go to the keychain.
        emptyKeys.remove(key);
        if (!passwordSafeAvailable) {
            memoryFallback.put(key, value);
            return;
        }
        try {
            // Use null userName so the lookup key matches CredentialKey.attributes() exactly
            // (which builds CredentialAttributes with userName=null). Mixing a real user-name
            // here and null on retrieval breaks the round-trip on some keychain backends.
            PasswordSafe.getInstance().set(key.attributes(), new Credentials(null, value));
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
        emptyKeys.remove(key);
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
