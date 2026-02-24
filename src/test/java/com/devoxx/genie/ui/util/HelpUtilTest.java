package com.devoxx.genie.ui.util;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HelpUtilTest {

    private List<CustomPrompt> customPrompts;

    @BeforeEach
    void setUp() {
        customPrompts = new ArrayList<>();
        customPrompts.add(new CustomPrompt("test", "Test prompt"));
        customPrompts.add(new CustomPrompt("explain", "Explain this code"));
        customPrompts.add(new CustomPrompt("review", "Review this code"));
    }

    @Test
    void testGetCustomPromptCommands() {
        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(customPrompts));

            String result = HelpUtil.getCustomPromptCommands();

            assertThat(result).contains("/test : Test prompt");
            assertThat(result).contains("/explain : Explain this code");
            assertThat(result).contains("/review : Review this code");
            assertThat(result).doesNotContain("##");
        }
    }

    @Test
    void testGetCustomPromptCommandsForWebView() {
        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(customPrompts));

            String result = HelpUtil.getCustomPromptCommandsForWebView();

            assertThat(result).contains("<li><span class=\"feature-name\">/test</span> : Test prompt</li>");
            assertThat(result).contains("<li><span class=\"feature-name\">/explain</span> : Explain this code</li>");
            assertThat(result).contains("<li><span class=\"feature-name\">/review</span> : Review this code</li>");
            assertThat(result).contains("feature-name");
        }
    }

    @Test
    void testGetCustomPromptCommandsForWebView_EmptyList() {
        List<CustomPrompt> emptyPrompts = new ArrayList<>();

        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(emptyPrompts));

            String result = HelpUtil.getCustomPromptCommandsForWebView();

            assertThat(result).isEmpty();
        }
    }

    @Test
    void testGetHelpMarkdown() {
        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(customPrompts));

            String result = HelpUtil.getHelpMarkdown();

            assertThat(result).contains("### Available commands");
            assertThat(result).contains("**/test** : Test prompt");
            assertThat(result).contains("**/explain** : Explain this code");
            assertThat(result).contains("**/review** : Review this code");
            assertThat(result).contains("GitHub");
            assertThat(result).contains("Bluesky");
        }
    }

    @Test
    void testGetHelpMarkdown_EmptyList() {
        List<CustomPrompt> emptyPrompts = new ArrayList<>();

        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(emptyPrompts));

            String result = HelpUtil.getHelpMarkdown();

            assertThat(result).contains("### Available commands");
            assertThat(result).doesNotContain("**/test**");
            assertThat(result).contains("GitHub");
            assertThat(result).contains("Bluesky");
        }
    }

    @Test
    void testGetHelpMessage() {
        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(customPrompts));

            String result = HelpUtil.getHelpMessage();

            assertThat(result).contains("<html>");
            assertThat(result).contains("<head>");
            assertThat(result).contains("<style type=\"text/css\">");
            assertThat(result).contains("body {");
            assertThat(result).contains("font-family: 'Source Code Pro', monospace;");
            assertThat(result).contains("Available commands:");
            assertThat(result).contains("/test : Test prompt");
            assertThat(result).contains("/explain : Explain this code");
            assertThat(result).contains("/review : Review this code");
            assertThat(result).contains("Devoxx Genie is open source");
            assertThat(result).contains("github.com/devoxx/DevoxxGenieIDEAPlugin");
            assertThat(result).contains("bsky.app/profile/devoxxgenie.bsky.social");
            assertThat(result).contains("</html>");
        }
    }

    @Test
    void testGetHelpMessage_WithCustomScaleFactor() {
        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(customPrompts));

            // The method uses JBUIScale.scale(1f) which should return 1.0f
            // So the zoom should be "normal"
            String result = HelpUtil.getHelpMessage();

            assertThat(result).contains("zoom: normal");
        }
    }

    @Test
    void testGetHelpMessage_WithMultipleCustomPrompts() {
        List<CustomPrompt> multiplePrompts = new ArrayList<>();
        multiplePrompts.add(new CustomPrompt("custom1", "Custom prompt 1"));
        multiplePrompts.add(new CustomPrompt("custom2", "Custom prompt 2"));
        multiplePrompts.add(new CustomPrompt("custom3", "Custom prompt 3"));

        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(multiplePrompts));

            String result = HelpUtil.getHelpMessage();

            assertThat(result).contains("/custom1 : Custom prompt 1");
            assertThat(result).contains("/custom2 : Custom prompt 2");
            assertThat(result).contains("/custom3 : Custom prompt 3");
        }
    }

    @Test
    void testGetCustomPromptCommands_WithSpecialCharacters() {
        List<CustomPrompt> specialPrompts = new ArrayList<>();
        specialPrompts.add(new CustomPrompt("test:with:colons", "Prompt with : colons"));
        specialPrompts.add(new CustomPrompt("test-with-dashes", "Prompt with - dashes"));

        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(specialPrompts));

            String result = HelpUtil.getCustomPromptCommands();

            assertThat(result).contains("/test:with:colons : Prompt with : colons");
            assertThat(result).contains("/test-with-dashes : Prompt with - dashes");
        }
    }

    @Test
    void testGetCustomPromptCommandsForWebView_WithSpecialCharacters() {
        List<CustomPrompt> specialPrompts = new ArrayList<>();
        specialPrompts.add(new CustomPrompt("test:with:colons", "Prompt with : colons"));
        specialPrompts.add(new CustomPrompt("test-with-dashes", "Prompt with - dashes"));

        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(specialPrompts));

            String result = HelpUtil.getCustomPromptCommandsForWebView();

            assertThat(result).contains("<span class=\"feature-name\">/test:with:colons</span>");
            assertThat(result).contains("<span class=\"feature-name\">/test-with-dashes</span>");
        }
    }

    @Test
    void testGetHelpMarkdown_WithSpecialCharacters() {
        List<CustomPrompt> specialPrompts = new ArrayList<>();
        specialPrompts.add(new CustomPrompt("test:with:colons", "Prompt with : colons"));
        specialPrompts.add(new CustomPrompt("test-with-dashes", "Prompt with - dashes"));

        try (MockedStatic<DevoxxGenieStateService> mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            mockedStateService.when(DevoxxGenieStateService::getInstance)
                    .thenReturn(createMockStateService(specialPrompts));

            String result = HelpUtil.getHelpMarkdown();

            assertThat(result).contains("**/test:with:colons** : Prompt with : colons");
            assertThat(result).contains("**/test-with-dashes** : Prompt with - dashes");
        }
    }

    // Helper method to create a mock state service
    private DevoxxGenieStateService createMockStateService(List<CustomPrompt> prompts) {
        DevoxxGenieStateService mockService = Mockito.mock(DevoxxGenieStateService.class);
        Mockito.when(mockService.getCustomPrompts()).thenReturn(prompts);
        return mockService;
    }
}