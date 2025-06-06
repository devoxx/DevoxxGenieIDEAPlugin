package com.devoxx.genie.service.mcp;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MCPExecutionService to verify NPX command handling fixes
 */
class MCPExecutionServiceTest {

    @Test
    void testCreateMCPCommand_WithValidCommand() {
        List<String> command = Arrays.asList("/usr/local/bin/npx", "-y", "@modelcontextprotocol/server-filesystem", "/path/to/project");
        
        List<String> result = MCPExecutionService.createMCPCommand(command);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            assertEquals("cmd.exe", result.get(0));
            assertEquals("/c", result.get(1));
        } else {
            assertEquals("/bin/bash", result.get(0));
            assertEquals("-c", result.get(1));
        }
        
        // The command string should contain all parts
        String commandStr = result.get(2);
        assertTrue(commandStr.contains("/usr/local/bin/npx"));
        assertTrue(commandStr.contains("-y"));
        assertTrue(commandStr.contains("@modelcontextprotocol/server-filesystem"));
        assertTrue(commandStr.contains("/path/to/project"));
    }

    @Test
    void testCreateMCPCommand_WithNullCommand_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            MCPExecutionService.createMCPCommand(null);
        });
    }

    @Test
    void testCreateMCPCommand_WithEmptyCommand_ThrowsException() {
        List<String> emptyCommand = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> {
            MCPExecutionService.createMCPCommand(emptyCommand);
        });
    }

    @Test
    void testCreateMCPCommand_WithNullArguments_FiltersOut() {
        List<String> commandWithNulls = Arrays.asList("/usr/local/bin/npx", null, "-y", null, "@modelcontextprotocol/server-filesystem");
        
        List<String> result = MCPExecutionService.createMCPCommand(commandWithNulls);
        
        assertNotNull(result);
        String commandStr = result.get(2);
        
        // Should contain non-null arguments
        assertTrue(commandStr.contains("/usr/local/bin/npx"));
        assertTrue(commandStr.contains("-y"));
        assertTrue(commandStr.contains("@modelcontextprotocol/server-filesystem"));
        
        // Should not contain "null" strings
        assertFalse(commandStr.contains("null"));
    }

    @Test
    void testCreateMCPCommand_WithArgumentsContainingSpaces() {
        List<String> command = Arrays.asList("/usr/local/bin/npx", "-y", "package with spaces", "/path/with spaces/project");
        
        List<String> result = MCPExecutionService.createMCPCommand(command);
        
        assertNotNull(result);
        String commandStr = result.get(2);
        
        // Arguments with spaces should be quoted
        assertTrue(commandStr.contains("\"package with spaces\""));
        assertTrue(commandStr.contains("\"/path/with spaces/project\""));
    }
}
