package com.devoxx.genie.ui.settings.completion;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompletionSettingsConfigurableTest {

    @Mock
    private Application application;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;

    private DevoxxGenieStateService stateService;
    private CompletionSettingsConfigurable configurable;

    @BeforeEach
    void setUp() {
        stateService = new DevoxxGenieStateService();

        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);
        lenient().doAnswer(invocation -> null).when(application).executeOnPooledThread(any(Runnable.class));

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        configurable = new CompletionSettingsConfigurable();
    }

    @AfterEach
    void tearDown() {
        stateServiceMockedStatic.close();
        applicationManagerMockedStatic.close();
    }

    @Test
    void shouldHaveCorrectDisplayName() {
        assertThat(configurable.getDisplayName()).isEqualTo("Inline Completion");
    }

    @Nested
    class BeforeCreateComponent {

        @Test
        void shouldReturnFalseForIsModifiedWhenNotCreated() {
            // When settingsComponent is null (before createComponent), isModified should return false
            assertThat(configurable.isModified()).isFalse();
        }

        @Test
        void shouldNotThrowOnApplyWhenNotCreated() {
            // Should handle null settingsComponent gracefully
            configurable.apply();
            // No exception means success
        }

        @Test
        void shouldNotThrowOnResetWhenNotCreated() {
            configurable.reset();
            // No exception means success
        }
    }

    @Nested
    class AfterCreateComponent {

        @BeforeEach
        void createComponent() {
            configurable.createComponent();
        }

        @Test
        void shouldCreateNonNullComponent() {
            assertThat(configurable.createComponent()).isNotNull();
        }

        @Test
        void shouldDelegateIsModified() {
            // Initially not modified since component matches state
            assertThat(configurable.isModified()).isFalse();
        }

        @Test
        void shouldDelegateApply() {
            configurable.apply();
            // Should not throw, state should remain consistent
            assertThat(stateService.getInlineCompletionProvider()).isNotNull();
        }

        @Test
        void shouldDelegateReset() {
            configurable.reset();
            // Should not throw
        }
    }

    @Nested
    class DisposeUIResources {

        @Test
        void shouldNullifyComponentOnDispose() {
            configurable.createComponent();
            configurable.disposeUIResources();

            // After dispose, isModified should return false (null guard)
            assertThat(configurable.isModified()).isFalse();
        }

        @Test
        void shouldNotThrowOnApplyAfterDispose() {
            configurable.createComponent();
            configurable.disposeUIResources();
            configurable.apply();
            // No exception means success
        }

        @Test
        void shouldNotThrowOnResetAfterDispose() {
            configurable.createComponent();
            configurable.disposeUIResources();
            configurable.reset();
            // No exception means success
        }
    }
}
