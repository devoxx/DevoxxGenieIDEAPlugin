package com.devoxx.genie.ui.settings.mcp.dialog.transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for StdioTransportPanel argument parsing fixes
 */
class StdioTransportPanelTest {

    private StdioTransportPanel panel;
    private JTextArea argsArea;

    @BeforeEach
    void setUp() throws Exception {
        panel = new StdioTransportPanel();
        
        // Use reflection to access the private argsArea field
        java.lang.reflect.Field argsAreaField = StdioTransportPanel.class.getDeclaredField("argsArea");
        argsAreaField.setAccessible(true);
        argsArea = (JTextArea) argsAreaField.get(panel);
    }

    @Test
    void testParseArguments_WithNPXScopedPackage() throws Exception {        // Set up NPX arguments with scoped package (each on new line)
        argsArea.setText("-y\n@modelcontextprotocol/server-filesystem\n/path/to/project");
        
        // Use reflection to call the private parseArguments method
        Method parseArgumentsMethod = StdioTransportPanel.class.getDeclaredMethod("parseArguments");
        parseArgumentsMethod.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) parseArgumentsMethod.invoke(panel);
        
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("-y", result.get(0));
        assertEquals("@modelcontextprotocol/server-filesystem", result.get(1));
        assertEquals("/path/to/project", result.get(2));
    }

    @Test
    void testParseArguments_WithNullText() throws Exception {
        // Set null text
        argsArea.setText(null);
        
        Method parseArgumentsMethod = StdioTransportPanel.class.getDeclaredMethod("parseArguments");
        parseArgumentsMethod.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) parseArgumentsMethod.invoke(panel);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseArguments_WithEmptyText() throws Exception {
        // Set empty text
        argsArea.setText("");
        
        Method parseArgumentsMethod = StdioTransportPanel.class.getDeclaredMethod("parseArguments");
        parseArgumentsMethod.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) parseArgumentsMethod.invoke(panel);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseArguments_WithWhitespaceOnlyText() throws Exception {
        // Set whitespace-only text
        argsArea.setText("   \n  \n  ");
        
        Method parseArgumentsMethod = StdioTransportPanel.class.getDeclaredMethod("parseArguments");
        parseArgumentsMethod.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) parseArgumentsMethod.invoke(panel);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
