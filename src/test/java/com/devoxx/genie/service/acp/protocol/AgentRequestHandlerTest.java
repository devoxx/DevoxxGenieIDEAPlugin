package com.devoxx.genie.service.acp.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, ACP!");

        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(1);
        msg.setMethod("fs/read_text_file");
        msg.setParams(AcpTransport.MAPPER.valueToTree(Map.of("path", testFile.toString())));

        handler.handle(msg);

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
        msg.setId(2);
        msg.setMethod("fs/write_text_file");
        msg.setParams(AcpTransport.MAPPER.valueToTree(Map.of(
                "path", testFile.toString(),
                "content", "Written by ACP"
        )));

        handler.handle(msg);

        assertThat(Files.readString(testFile)).isEqualTo("Written by ACP");

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
        msg.setId(3);
        msg.setMethod("fs/write_text_file");
        msg.setParams(AcpTransport.MAPPER.valueToTree(Map.of(
                "path", testFile.toString(),
                "content", "nested"
        )));

        handler.handle(msg);

        assertThat(Files.exists(testFile)).isTrue();
        assertThat(Files.readString(testFile)).isEqualTo("nested");
    }

    @Test
    void testHandleRequestPermission_autoApproves() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(4);
        msg.setMethod("session/request_permission");
        msg.setParams(AcpTransport.MAPPER.valueToTree(Map.of("permission", "fs_write")));

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
        msg.setId(5);
        msg.setMethod("unknown/method");
        msg.setParams(AcpTransport.MAPPER.valueToTree(Map.of()));

        handler.handle(msg);

        verify(mockTransport).sendErrorResponse(eq(5), eq(-32603), contains("Unknown method"));
    }

    @Test
    void testHandleFsRead_nonExistentFile_sendsError() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(6);
        msg.setMethod("fs/read_text_file");
        msg.setParams(AcpTransport.MAPPER.valueToTree(Map.of("path", "/nonexistent/file.txt")));

        handler.handle(msg);

        verify(mockTransport).sendErrorResponse(eq(6), eq(-32603), any());
    }

    @Test
    void testHandleTerminalCreate_startsProcess() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(7);
        msg.setMethod("terminal/create");
        msg.setParams(AcpTransport.MAPPER.valueToTree(Map.of(
                "command", "echo hello",
                "cwd", tempDir.toString()
        )));

        handler.handle(msg);

        verify(mockTransport).sendResponse(eq(7), argThat(result -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return map.containsKey("terminalId");
        }));
    }

    @Test
    void testHandleTerminalOutput_unknownTerminal() throws Exception {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(8);
        msg.setMethod("terminal/output");
        msg.setParams(AcpTransport.MAPPER.valueToTree(Map.of("terminalId", "nonexistent")));

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
        msg.setId(9);
        msg.setMethod("terminal/kill");
        msg.setParams(AcpTransport.MAPPER.valueToTree(Map.of("terminalId", "nonexistent")));

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
