package com.devoxx.genie.service.acp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentRequestHandlerTest {

    private AcpTransport mockTransport;
    private AgentRequestHandler handler;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mockTransport = mock(AcpTransport.class);
        handler = new AgentRequestHandler(mockTransport);
    }

    @Test
    void testHandleFsRead_readsFileContent() throws Exception {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, ACP!");

        // Build a request message
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = 1;
        msg.method = "fs/read_text_file";
        msg.params = AcpTransport.MAPPER.valueToTree(Map.of("path", testFile.toString()));

        handler.handle(msg);

        // Verify response was sent with file content
        verify(mockTransport).sendResponse(eq(1), argThat(result -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return "Hello, ACP!".equals(map.get("content"));
        }));
    }

    @Test
    void testHandleFsWrite_writesFileContent() throws Exception {
        Path testFile = tempDir.resolve("output.txt");

        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = 2;
        msg.method = "fs/write_text_file";
        msg.params = AcpTransport.MAPPER.valueToTree(Map.of(
                "path", testFile.toString(),
                "content", "Written by ACP"
        ));

        handler.handle(msg);

        // Verify file was written
        assertThat(Files.readString(testFile)).isEqualTo("Written by ACP");

        // Verify success response
        verify(mockTransport).sendResponse(eq(2), argThat(result -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return Boolean.TRUE.equals(map.get("success"));
        }));
    }

    @Test
    void testHandleFsWrite_createsParentDirectories() throws Exception {
        Path testFile = tempDir.resolve("sub/dir/file.txt");

        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = 3;
        msg.method = "fs/write_text_file";
        msg.params = AcpTransport.MAPPER.valueToTree(Map.of(
                "path", testFile.toString(),
                "content", "nested"
        ));

        handler.handle(msg);

        assertThat(Files.exists(testFile)).isTrue();
        assertThat(Files.readString(testFile)).isEqualTo("nested");
    }

    @Test
    void testHandleRequestPermission_autoApproves() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = 4;
        msg.method = "session/request_permission";
        msg.params = AcpTransport.MAPPER.valueToTree(Map.of("permission", "fs_write"));

        handler.handle(msg);

        verify(mockTransport).sendResponse(eq(4), argThat(result -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return Boolean.TRUE.equals(map.get("granted"));
        }));
    }

    @Test
    void testHandleUnknownMethod_sendsError() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = 5;
        msg.method = "unknown/method";
        msg.params = AcpTransport.MAPPER.valueToTree(Map.of());

        handler.handle(msg);

        verify(mockTransport).sendErrorResponse(eq(5), eq(-32603), contains("Unknown method"));
    }

    @Test
    void testHandleFsRead_nonExistentFile_sendsError() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = 6;
        msg.method = "fs/read_text_file";
        msg.params = AcpTransport.MAPPER.valueToTree(Map.of("path", "/nonexistent/file.txt"));

        handler.handle(msg);

        // Should send an error response because the file doesn't exist
        verify(mockTransport).sendErrorResponse(eq(6), eq(-32603), any());
    }

    @Test
    void testHandleTerminalCreate_startsProcess() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = 7;
        msg.method = "terminal/create";
        msg.params = AcpTransport.MAPPER.valueToTree(Map.of(
                "command", "echo hello",
                "cwd", tempDir.toString()
        ));

        handler.handle(msg);

        // Verify a response with terminalId was sent
        verify(mockTransport).sendResponse(eq(7), argThat(result -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return map.containsKey("terminalId");
        }));
    }

    @Test
    void testHandleTerminalOutput_unknownTerminal() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = 8;
        msg.method = "terminal/output";
        msg.params = AcpTransport.MAPPER.valueToTree(Map.of("terminalId", "nonexistent"));

        handler.handle(msg);

        verify(mockTransport).sendResponse(eq(8), argThat(result -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return map.containsKey("error");
        }));
    }

    @Test
    void testHandleTerminalKill_unknownTerminal_noError() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.id = 9;
        msg.method = "terminal/kill";
        msg.params = AcpTransport.MAPPER.valueToTree(Map.of("terminalId", "nonexistent"));

        handler.handle(msg);

        verify(mockTransport).sendResponse(eq(9), argThat(result -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return Boolean.TRUE.equals(map.get("success"));
        }));
    }

    private static String contains(String substring) {
        return argThat(s -> s != null && s.contains(substring));
    }
}
