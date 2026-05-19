package com.devoxx.genie.startup;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Belt-and-braces fallback for the plaintext-credential migration. The primary
 * migration runs synchronously inside {@link DevoxxGenieStateService#loadState}
 * the first time settings are deserialised after the upgrade. This activity
 * re-runs the migration on each project open in case the previous attempt was
 * deferred (e.g. PasswordSafe was unavailable or threw at that moment). The
 * migration is idempotent and a no-op once {@code credentialsMigratedV1} is
 * set.
 */
public class CredentialMigrationStartupActivity implements ProjectActivity {

    private static final Logger LOG = Logger.getInstance(CredentialMigrationStartupActivity.class);

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        try {
            DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
            if (!state.isCredentialsMigratedV1()) {
                state.migrateCredentialsToPasswordSafe();
            }
        } catch (Throwable t) {
            // Migration must never break IDE startup.
            LOG.warn("Credential migration retry failed during project startup", t);
        }
        return Unit.INSTANCE;
    }
}
