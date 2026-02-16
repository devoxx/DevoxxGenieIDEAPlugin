package com.devoxx.genie.completion;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InlineCompletionServiceTest {

    private InlineCompletionService service;
    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<ApplicationManager> mockedAppManager;
    private DevoxxGenieStateService mockState;
    private CompletionCache mockCache;
    private OllamaFimProvider mockOllamaProvider;
    private LMStudioFimProvider mockLmStudioProvider;

    @BeforeEach
    void setUp() throws Exception {
        mockState = mock(DevoxxGenieStateService.class);
        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(mockState);

        // Create service using constructor directly (bypassing ApplicationManager.getService)
        service = new InlineCompletionService();

        // Replace internal fields with mocks for testability
        mockCache = mock(CompletionCache.class);
        mockOllamaProvider = mock(OllamaFimProvider.class);
        mockLmStudioProvider = mock(LMStudioFimProvider.class);

        setField(service, "cache", mockCache);
        setField(service, "ollamaProvider", mockOllamaProvider);
        setField(service, "lmStudioProvider", mockLmStudioProvider);
    }

    @AfterEach
    void tearDown() {
        if (mockedStateService != null) mockedStateService.close();
        if (mockedAppManager != null) mockedAppManager.close();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void getCompletionShouldReturnCachedResultIfAvailable() {
        when(mockCache.get("prefix", "suffix")).thenReturn("cached completion");

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isEqualTo("cached completion");

        // Should not call any provider
        verifyNoInteractions(mockOllamaProvider);
        verifyNoInteractions(mockLmStudioProvider);
    }

    @Test
    void getCompletionShouldReturnNullWhenProviderNameIsNull() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn(null);

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isNull();
    }

    @Test
    void getCompletionShouldReturnNullWhenProviderNameIsBlank() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("   ");

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isNull();
    }

    @Test
    void getCompletionShouldReturnNullWhenModelNameIsNull() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("Ollama");
        when(mockState.getInlineCompletionModel()).thenReturn(null);

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isNull();
    }

    @Test
    void getCompletionShouldReturnNullWhenModelNameIsBlank() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("Ollama");
        when(mockState.getInlineCompletionModel()).thenReturn("  ");

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isNull();
    }

    @Test
    void getCompletionShouldReturnNullForUnknownProvider() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("UnknownProvider");
        when(mockState.getInlineCompletionModel()).thenReturn("some-model");

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isNull();
    }

    @Test
    void getCompletionShouldUseOllamaProvider() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("Ollama");
        when(mockState.getInlineCompletionModel()).thenReturn("starcoder2");
        when(mockState.getOllamaModelUrl()).thenReturn("http://localhost:11434/");
        when(mockState.getInlineCompletionTemperature()).thenReturn(0.0);
        when(mockState.getInlineCompletionMaxTokens()).thenReturn(64);
        when(mockState.getInlineCompletionTimeoutMs()).thenReturn(5000);

        FimResponse response = new FimResponse("completion text", 100);
        when(mockOllamaProvider.generate(any(FimRequest.class))).thenReturn(response);

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isEqualTo("completion text");

        verify(mockOllamaProvider).generate(any(FimRequest.class));
        verify(mockCache).put("prefix", "suffix", "completion text");
    }

    @Test
    void getCompletionShouldUseLMStudioProvider() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("LMStudio");
        when(mockState.getInlineCompletionModel()).thenReturn("codellama");
        when(mockState.getLmstudioModelUrl()).thenReturn("http://localhost:1234/v1/");
        when(mockState.getInlineCompletionTemperature()).thenReturn(0.0);
        when(mockState.getInlineCompletionMaxTokens()).thenReturn(64);
        when(mockState.getInlineCompletionTimeoutMs()).thenReturn(5000);

        FimResponse response = new FimResponse("lmstudio completion", 50);
        when(mockLmStudioProvider.generate(any(FimRequest.class))).thenReturn(response);

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isEqualTo("lmstudio completion");

        verify(mockLmStudioProvider).generate(any(FimRequest.class));
        verify(mockCache).put("prefix", "suffix", "lmstudio completion");
    }

    @Test
    void getCompletionShouldReturnNullWhenProviderReturnsNull() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("Ollama");
        when(mockState.getInlineCompletionModel()).thenReturn("starcoder2");
        when(mockState.getOllamaModelUrl()).thenReturn("http://localhost:11434/");
        when(mockState.getInlineCompletionTemperature()).thenReturn(0.0);
        when(mockState.getInlineCompletionMaxTokens()).thenReturn(64);
        when(mockState.getInlineCompletionTimeoutMs()).thenReturn(5000);

        when(mockOllamaProvider.generate(any(FimRequest.class))).thenReturn(null);

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isNull();

        verify(mockCache, never()).put(anyString(), anyString(), anyString());
    }

    @Test
    void getCompletionShouldReturnNullWhenResponseHasEmptyText() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("Ollama");
        when(mockState.getInlineCompletionModel()).thenReturn("starcoder2");
        when(mockState.getOllamaModelUrl()).thenReturn("http://localhost:11434/");
        when(mockState.getInlineCompletionTemperature()).thenReturn(0.0);
        when(mockState.getInlineCompletionMaxTokens()).thenReturn(64);
        when(mockState.getInlineCompletionTimeoutMs()).thenReturn(5000);

        FimResponse response = new FimResponse("", 50);
        when(mockOllamaProvider.generate(any(FimRequest.class))).thenReturn(response);

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isNull();
    }

    @Test
    void getCompletionShouldUseDefaultValuesWhenSettingsAreNull() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("Ollama");
        when(mockState.getInlineCompletionModel()).thenReturn("starcoder2");
        when(mockState.getOllamaModelUrl()).thenReturn("http://localhost:11434/");
        when(mockState.getInlineCompletionTemperature()).thenReturn(null);
        when(mockState.getInlineCompletionMaxTokens()).thenReturn(null);
        when(mockState.getInlineCompletionTimeoutMs()).thenReturn(null);

        FimResponse response = new FimResponse("completion", 100);
        when(mockOllamaProvider.generate(any(FimRequest.class))).thenReturn(response);

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isEqualTo("completion");
    }

    @Test
    void getCompletionShouldUseDefaultOllamaUrlWhenNull() {
        when(mockCache.get("prefix", "suffix")).thenReturn(null);
        when(mockState.getInlineCompletionProvider()).thenReturn("Ollama");
        when(mockState.getInlineCompletionModel()).thenReturn("starcoder2");
        when(mockState.getOllamaModelUrl()).thenReturn(null);
        when(mockState.getInlineCompletionTemperature()).thenReturn(0.0);
        when(mockState.getInlineCompletionMaxTokens()).thenReturn(64);
        when(mockState.getInlineCompletionTimeoutMs()).thenReturn(5000);

        FimResponse response = new FimResponse("completion", 100);
        when(mockOllamaProvider.generate(any(FimRequest.class))).thenReturn(response);

        String result = service.getCompletion("prefix", "suffix");
        assertThat(result).isEqualTo("completion");
    }

    @Test
    void cancelActiveRequestsShouldCancelBothProviders() {
        service.cancelActiveRequests();

        verify(mockOllamaProvider).cancelActiveCall();
        verify(mockLmStudioProvider).cancelActiveCall();
    }

    @Test
    void clearCacheShouldDelegateToCache() {
        service.clearCache();

        verify(mockCache).clear();
    }
}
