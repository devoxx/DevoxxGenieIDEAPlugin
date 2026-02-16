package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.ui.webview.WebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MCPMessageTemplateTest {

    @Mock
    private WebServer webServer;

    @BeforeEach
    void setUp() {
    }

    @Test
    void generate_withNullMessage_returnsEmptyString() {
        MCPMessageTemplate template = new MCPMessageTemplate(webServer, null);

        String result = template.generate();

        assertThat(result).isEmpty();
    }

    @Test
    void generate_withNullContent_returnsEmptyString() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.AI_MSG)
                .content(null)
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).isEmpty();
    }

    @Test
    void generate_withAIMessage_returnsAIMessageDiv() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.AI_MSG)
                .content("Hello from AI")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("ai-message");
        assertThat(result).contains("Hello from AI");
    }

    @Test
    void generate_withAIMessage_escapesHtml() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.AI_MSG)
                .content("Use tags like <b>bold</b>")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).doesNotContain("<b>");
        assertThat(result).contains("&lt;b&gt;");
    }

    @Test
    void generate_withToolMessage_returnsToolMessageDiv() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.TOOL_MSG)
                .content("Tool output result")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("tool-message");
        assertThat(result).contains("Tool output result");
    }

    @Test
    void generate_withToolMessage_containsToggleAndCopyButton() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.TOOL_MSG)
                .content("Some tool output")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("toggle-icon");
        assertThat(result).contains("toggleToolContent");
        assertThat(result).contains("copy-tool-button");
        assertThat(result).contains("copyToolOutput");
    }

    @Test
    void generate_withToolMessageContainingJSON_detectsDataType() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.TOOL_MSG)
                .content("{\"key\": \"value\"}")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("badge-data");
        assertThat(result).contains("Data");
    }

    @Test
    void generate_withToolMessageContainingFunction_detectsFunctionType() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.TOOL_MSG)
                .content("function doSomething() { return true; }")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("badge-function");
        assertThat(result).contains("Function");
    }

    @Test
    void generate_withToolMessageContainingDefKeyword_detectsFunctionType() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.TOOL_MSG)
                .content("def my_function():\n    pass")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("badge-function");
    }

    @Test
    void generate_withToolMessagePlainText_detectsToolType() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.TOOL_MSG)
                .content("Just some plain text output")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("badge-tool");
        assertThat(result).contains("Tool");
    }

    @Test
    void generate_withLogMessage_returnsLogMessageDiv() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.LOG_MSG)
                .content("Some log message")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("tool-message");
        assertThat(result).contains("general-log");
        assertThat(result).contains("Log");
        assertThat(result).contains("Some log message");
    }

    @Test
    void generate_withLogMessageStartingWithLessThan_detectedAsInput() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.LOG_MSG)
                .content("<some input data")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("input-log");
        assertThat(result).contains("Input");
    }

    @Test
    void generate_withLogMessageStartingWithGreaterThan_detectedAsOutput() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.LOG_MSG)
                .content(">some output data")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("output-log");
        assertThat(result).contains("Output");
    }

    @Test
    void generate_withLogMessage_containsCopyButton() {
        MCPMessage message = MCPMessage.builder()
                .type(MCPType.LOG_MSG)
                .content("Log data here")
                .build();

        MCPMessageTemplate template = new MCPMessageTemplate(webServer, message);

        String result = template.generate();

        assertThat(result).contains("copy-tool-button");
        assertThat(result).contains("badge-log");
    }
}
