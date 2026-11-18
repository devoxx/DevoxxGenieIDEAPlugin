package com.devoxx.genie.ui.settings;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.credentials.CredentialService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * The one-shot plaintext -> PasswordSafe credential migration. Keys added after that migration shipped
 * (e.g. Keenable, TASK-277.3) have no legacy XML field; they must not make the migration fail forever.
 */
class DevoxxGenieStateServiceCredentialMigrationTest {

    /** An available credential store, as PasswordSafe is in a running IDE. */
    static final class AvailableCredentialService implements CredentialService {
        final Map<String, String> store = new ConcurrentHashMap<>();

        @Override public @NotNull String getCredential(@NotNull CredentialKey key) { return getCredential(key.getSubKey()); }
        @Override public void setCredential(@NotNull CredentialKey key, @Nullable String value) { setCredential(key.getSubKey(), value); }
        @Override public void removeCredential(@NotNull CredentialKey key) { removeCredential(key.getSubKey()); }
        @Override public @NotNull String getCredential(@NotNull String subKey) { return store.getOrDefault(subKey, ""); }
        @Override public void setCredential(@NotNull String subKey, @Nullable String value) {
            if (value == null || value.isEmpty()) store.remove(subKey); else store.put(subKey, value);
        }
        @Override public void removeCredential(@NotNull String subKey) { store.remove(subKey); }
        @Override public boolean isAvailable() { return true; }
    }

    private MockedStatic<ApplicationManager> applicationManagerStatic;
    private final AvailableCredentialService credentials = new AvailableCredentialService();
    private DevoxxGenieStateService state;

    @BeforeEach
    void setUp() {
        Application application = mock(Application.class);
        when(application.getService(CredentialService.class)).thenReturn(credentials);
        applicationManagerStatic = mockStatic(ApplicationManager.class);
        applicationManagerStatic.when(ApplicationManager::getApplication).thenReturn(application);
        state = new DevoxxGenieStateService();
        state.setCredentialsMigratedV1(false);
    }

    @AfterEach
    void tearDown() {
        applicationManagerStatic.close();
    }

    private void setLegacyPlaintext(String fieldName, String value) throws Exception {
        Field field = DevoxxGenieStateService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(state, value);
    }

    private Object legacyPlaintext(String fieldName) throws Exception {
        Field field = DevoxxGenieStateService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(state);
    }

    @Test
    void migration_completes_evenWithKeysThatHaveNoLegacyField() throws Exception {
        setLegacyPlaintext("tavilySearchKey", "tvly-plaintext");
        setLegacyPlaintext("googleSearchKey", "g-plaintext");

        state.migrateCredentialsToPasswordSafe();

        assertThat(state.isCredentialsMigratedV1()).as("migration marked complete").isTrue();
        assertThat(credentials.getCredential(CredentialKey.TAVILY_SEARCH_KEY)).isEqualTo("tvly-plaintext");
        assertThat(credentials.getCredential(CredentialKey.GOOGLE_SEARCH_KEY)).isEqualTo("g-plaintext");
        assertThat(legacyPlaintext("tavilySearchKey")).as("plaintext wiped").isEqualTo("");
        assertThat(legacyPlaintext("googleSearchKey")).isEqualTo("");
    }

    @Test
    void keenableKey_hasNoLegacyField_andIsLeftUntouched() throws Exception {
        credentials.setCredential(CredentialKey.KEENABLE_SEARCH_KEY, "keenable-in-passwordsafe");

        state.migrateCredentialsToPasswordSafe();

        assertThat(state.isCredentialsMigratedV1()).isTrue();
        assertThat(credentials.getCredential(CredentialKey.KEENABLE_SEARCH_KEY)).isEqualTo("keenable-in-passwordsafe");
    }

    @Test
    void existingPasswordSafeValue_isNotOverwritten_butPlaintextIsStillWiped() throws Exception {
        credentials.setCredential(CredentialKey.TAVILY_SEARCH_KEY, "already-safe");
        setLegacyPlaintext("tavilySearchKey", "stale-plaintext");

        state.migrateCredentialsToPasswordSafe();

        assertThat(credentials.getCredential(CredentialKey.TAVILY_SEARCH_KEY)).isEqualTo("already-safe");
        assertThat(legacyPlaintext("tavilySearchKey")).isEqualTo("");
    }
}
