package com.devoxx.genie.service;

import com.devoxx.genie.controller.listener.TokenCalculationListener;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenCalculationServiceTest {

    @Mock
    private Project project;

    @Mock
    private VirtualFile directory;

    @Mock
    private TokenCalculationListener listener;

    @Mock
    private ProjectContentService projectContentService;

    @Mock
    private Application application;

    @Mock
    private LLMModelRegistryService modelRegistryService;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<ProjectContentService> projectContentServiceMockedStatic;
    private MockedStatic<NotificationUtil> notificationUtilMockedStatic;
    private MockedStatic<LLMModelRegistryService> modelRegistryMockedStatic;

    private TokenCalculationService tokenCalculationService;

    @BeforeEach
    void setUp() {
        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);

        // Make invokeLater execute immediately
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(application).invokeLater(any(Runnable.class));

        projectContentServiceMockedStatic = Mockito.mockStatic(ProjectContentService.class);
        projectContentServiceMockedStatic.when(ProjectContentService::getInstance).thenReturn(projectContentService);

        notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class);

        modelRegistryMockedStatic = Mockito.mockStatic(LLMModelRegistryService.class);
        modelRegistryMockedStatic.when(LLMModelRegistryService::getInstance).thenReturn(modelRegistryService);

        tokenCalculationService = new TokenCalculationService();
    }

    @AfterEach
    void tearDown() {
        applicationManagerMockedStatic.close();
        projectContentServiceMockedStatic.close();
        notificationUtilMockedStatic.close();
        modelRegistryMockedStatic.close();
    }

    private LanguageModel createLocalLanguageModel() {
        return LanguageModel.builder()
                .provider(ModelProvider.Ollama)
                .modelName("llama3")
                .displayName("Llama 3")
                .inputMaxTokens(8000)
                .build();
    }

    private ScanContentResult createScanResult(int tokenCount, int fileCount, int skippedFileCount, int skippedDirectoryCount) {
        ScanContentResult result = new ScanContentResult();
        result.setTokenCount(tokenCount);
        result.setFileCount(fileCount);
        result.setSkippedFileCount(skippedFileCount);
        result.setSkippedDirectoryCount(skippedDirectoryCount);
        return result;
    }

    @Test
    void testCalculateTokensAndCost_WithDirectory_ShowCostFalse() {
        ScanContentResult scanResult = createScanResult(500, 10, 2, 0);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);
        when(directory.getName()).thenReturn("src");

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 1000, ModelProvider.Ollama, null, false, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("src");
        assertThat(message).contains("500");
        assertThat(message).contains("10 files");
        assertThat(message).contains("skipped 2 files");
    }

    @Test
    void testCalculateTokensAndCost_WithoutDirectory_ShowCostFalse() {
        ScanContentResult scanResult = createScanResult(500, 10, 2, 0);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getProjectContent(eq(project), anyInt(), eq(true))).thenReturn(future);

        tokenCalculationService.calculateTokensAndCost(
                project, null, 1000, ModelProvider.Ollama, null, false, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("Project");
    }

    @Test
    void testCalculateTokensAndCost_LargeTokenCount_FormatsAsK() {
        ScanContentResult scanResult = createScanResult(5000, 20, 3, 0);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);
        when(directory.getName()).thenReturn("src");

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, null, false, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        // The format uses locale-dependent decimal separator (e.g. "5.0K" or "5,0K")
        assertThat(message).containsPattern("5[.,]0K");
    }

    @Test
    void testCalculateTokensAndCost_WithSkippedDirectories() {
        ScanContentResult scanResult = createScanResult(1500, 15, 5, 3);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);
        when(directory.getName()).thenReturn("src");

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, null, false, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("3 directories");
        assertThat(message).contains("skipped 5 files");
    }

    @Test
    void testCalculateTokensAndCost_WithSkippedFileExtensions() {
        ScanContentResult scanResult = createScanResult(1000, 10, 2, 0);
        Map<String, String> skippedFiles = new HashMap<>();
        skippedFiles.put("/path/to/file.png", "extension not in included list");
        skippedFiles.put("/path/to/data.csv", "extension not supported");
        scanResult.setSkippedFiles(skippedFiles);

        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);
        when(directory.getName()).thenReturn("src");

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, null, false, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("Skipped files extensions");
        assertThat(message).contains("png");
    }

    @Test
    void testCalculateTokensAndCost_ShowCostTrue_LocalProvider() {
        ScanContentResult scanResult = createScanResult(5000, 20, 3, 1);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);

        LanguageModel localModel = LanguageModel.builder()
                .provider(ModelProvider.Ollama)
                .modelName("llama3")
                .displayName("Llama 3")
                .inputMaxTokens(8000)
                .build();

        // Ollama is not an API key based provider
        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, localModel, true, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        // For local providers with showCost=true, it should show the default message without estimated cost
        assertThat(message).contains("tokens");
        assertThat(message).contains("files");
    }

    @Test
    void testCalculateTokensAndCost_ShowCostTrue_CloudProvider_WithInputCost() {
        ScanContentResult scanResult = createScanResult(10000, 30, 0, 0);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);

        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .inputCost(30.0)
                .inputMaxTokens(128000)
                .build();

        when(modelRegistryService.getModels()).thenReturn(List.of(languageModel));

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 128000, ModelProvider.OpenAI, languageModel, true, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("Estimated cost");
        assertThat(message).contains("OpenAI");
        assertThat(message).contains("GPT-4");
        assertThat(message).contains("$");
    }

    @Test
    void testCalculateTokensAndCost_ShowCostTrue_CloudProvider_NoInputCostFound() {
        ScanContentResult scanResult = createScanResult(10000, 30, 0, 0);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);

        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("unknown-model")
                .displayName("Unknown Model")
                .inputCost(0.0)
                .inputMaxTokens(128000)
                .build();

        // Return empty list so no matching model is found
        when(modelRegistryService.getModels()).thenReturn(List.of());

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 128000, ModelProvider.OpenAI, languageModel, true, listener);

        // When no input cost is found, a notification should be sent
        notificationUtilMockedStatic.verify(() -> NotificationUtil.sendNotification(eq(project), any()));
    }

    @Test
    void testCalculateTokensAndCost_ShowCostTrue_CloudProvider_ExceedsContextWindow() {
        ScanContentResult scanResult = createScanResult(200000, 100, 5, 2);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);

        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .inputCost(30.0)
                .inputMaxTokens(128000)
                .build();

        when(modelRegistryService.getModels()).thenReturn(List.of(languageModel));

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 128000, ModelProvider.OpenAI, languageModel, true, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("exceeds model's max context");
    }

    @Test
    void testCalculateTokensAndCost_ShowCostTrue_CloudProvider_NoSkippedFiles() {
        ScanContentResult scanResult = createScanResult(5000, 20, 0, 0);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);

        LanguageModel languageModel = LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .inputCost(30.0)
                .inputMaxTokens(128000)
                .build();

        when(modelRegistryService.getModels()).thenReturn(List.of(languageModel));

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 128000, ModelProvider.OpenAI, languageModel, true, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("Project contains");
        assertThat(message).contains("Estimated cost");
    }

    @Test
    void testCalculateTokensAndCost_DefaultMessage_WithSkippedFilesOnly() {
        ScanContentResult scanResult = createScanResult(1000, 10, 5, 0);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, createLocalLanguageModel(), true, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("skipped");
        assertThat(message).contains("5 files");
        assertThat(message).doesNotContain("directories");
    }

    @Test
    void testCalculateTokensAndCost_DefaultMessage_WithSkippedDirectoriesOnly() {
        ScanContentResult scanResult = createScanResult(1000, 10, 0, 3);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, createLocalLanguageModel(), true, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("skipped");
        assertThat(message).contains("3 directories");
    }

    @Test
    void testCalculateTokensAndCost_DefaultMessage_WithBothSkippedFilesAndDirectories() {
        ScanContentResult scanResult = createScanResult(1000, 10, 5, 3);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, createLocalLanguageModel(), true, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("5 files");
        assertThat(message).contains("and");
        assertThat(message).contains("3 directories");
    }

    @Test
    void testCalculateTokensAndCost_DefaultMessage_NoSkippedAnything() {
        ScanContentResult scanResult = createScanResult(1000, 10, 0, 0);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, createLocalLanguageModel(), true, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).doesNotContain("skipped");
    }

    @Test
    void testCalculateTokensAndCost_SkippedDirectoriesInMessage() {
        ScanContentResult scanResult = createScanResult(1000, 10, 2, 2);
        Map<String, String> skippedFiles = new HashMap<>();
        skippedFiles.put("/path/build/output.jar", "excluded by settings");
        skippedFiles.put("/path/node_modules/pkg.js", "excluded by settings");
        scanResult.setSkippedFiles(skippedFiles);

        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);
        when(directory.getName()).thenReturn("project");

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, null, false, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("Skipped directories");
    }

    @Test
    void testCalculateTokensAndCost_SmallTokenCount_ShowsExactNumber() {
        ScanContentResult scanResult = createScanResult(500, 5, 0, 0);
        CompletableFuture<ScanContentResult> future = CompletableFuture.completedFuture(scanResult);
        when(projectContentService.getDirectoryContent(eq(project), eq(directory), anyInt(), eq(true))).thenReturn(future);
        when(directory.getName()).thenReturn("src");

        tokenCalculationService.calculateTokensAndCost(
                project, directory, 10000, ModelProvider.Ollama, null, false, listener);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(listener).onTokenCalculationComplete(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertThat(message).contains("500 tokens");
    }
}
