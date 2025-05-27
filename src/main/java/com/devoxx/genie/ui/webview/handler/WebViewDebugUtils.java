package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.ui.webview.JCEFChecker;
import com.intellij.ui.jcef.JBCefBrowser;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * Debug utility for WebView issues.
 * This class provides methods to diagnose and test WebView functionality.
 */
@Slf4j
public class WebViewDebugUtils {
    
    /**
     * Perform comprehensive WebView diagnostics.
     */
    public static String performDiagnostics(JBCefBrowser browser) {
        StringBuilder diagnostics = new StringBuilder();
        diagnostics.append("=== WebView Diagnostics Report ===\n");
        diagnostics.append("Timestamp: ").append(new java.util.Date()).append("\n\n");
        
        // JCEF availability
        boolean jcefAvailable = JCEFChecker.isJCEFAvailable();
        diagnostics.append("JCEF Available: ").append(jcefAvailable).append("\n");
        
        if (!jcefAvailable) {
            diagnostics.append("ERROR: JCEF is not available - this explains the black rectangle issue\n");
            diagnostics.append("SOLUTION: Enable JCEF in IDE settings\n\n");
            return diagnostics.toString();
        }
        
        // Browser state
        if (browser == null) {
            diagnostics.append("ERROR: Browser is null\n");
            diagnostics.append("SOLUTION: Browser initialization failed\n\n");
            return diagnostics.toString();
        }
        
        diagnostics.append("Browser: ").append(browser.getClass().getSimpleName()).append("\n");
        
        // CEF Browser state
        try {
            if (browser.getCefBrowser() != null) {
                String url = browser.getCefBrowser().getURL();
                diagnostics.append("Current URL: ").append(url != null ? url : "null").append("\n");
                diagnostics.append("CEF Browser: Available\n");
            } else {
                diagnostics.append("ERROR: CEF Browser is null\n");
                diagnostics.append("POSSIBLE CAUSE: Browser not fully initialized\n");
            }
        } catch (Exception e) {
            diagnostics.append("ERROR: Exception accessing CEF Browser: ").append(e.getMessage()).append("\n");
        }
        
        // Component state
        try {
            JComponent component = browser.getComponent();
            if (component != null) {
                diagnostics.append("Component State:\n");
                diagnostics.append("  - Visible: ").append(component.isVisible()).append("\n");
                diagnostics.append("  - Showing: ").append(component.isShowing()).append("\n");
                diagnostics.append("  - Displayable: ").append(component.isDisplayable()).append("\n");
                diagnostics.append("  - Size: ").append(component.getWidth()).append("x").append(component.getHeight()).append("\n");
                diagnostics.append("  - Opaque: ").append(component.isOpaque()).append("\n");
                
                Color bg = component.getBackground();
                if (bg != null) {
                    diagnostics.append("  - Background: ").append(bg).append("\n");
                    if (bg.equals(Color.BLACK)) {
                        diagnostics.append("  - WARNING: Background is black - potential black rectangle!\n");
                    }
                }
            } else {
                diagnostics.append("ERROR: Component is null\n");
            }
        } catch (Exception e) {
            diagnostics.append("ERROR: Exception accessing component: ").append(e.getMessage()).append("\n");
        }
        
        // System information
        diagnostics.append("\nSystem Information:\n");
        diagnostics.append("  - OS: ").append(System.getProperty("os.name")).append("\n");
        diagnostics.append("  - Java Version: ").append(System.getProperty("java.version")).append("\n");
        diagnostics.append("  - Available Processors: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        diagnostics.append("  - Free Memory: ").append(Runtime.getRuntime().freeMemory() / 1024 / 1024).append(" MB\n");
        
        return diagnostics.toString();
    }
    
    /**
     * Test WebView functionality by injecting test JavaScript.
     */
    public static void performFunctionalityTest(JBCefBrowser browser) {
        if (!JCEFChecker.isJCEFAvailable() || browser == null || browser.getCefBrowser() == null) {
            log.warn("Cannot perform functionality test - browser not available");
            return;
        }
        
        String testJs = 
            "try { " +
            "  console.log('=== WebView Functionality Test ==='); " +
            "  " +
            "  var testResults = { " +
            "    timestamp: Date.now(), " +
            "    documentReady: document.readyState, " +
            "    bodyExists: !!document.body, " +
            "    containerExists: !!document.getElementById('conversation-container'), " +
            "    windowSize: { width: window.innerWidth, height: window.innerHeight }, " +
            "    documentSize: { width: document.documentElement.scrollWidth, height: document.documentElement.scrollHeight }, " +
            "    visibilityState: document.visibilityState, " +
            "    userAgent: navigator.userAgent " +
            "  }; " +
            "  " +
            "  if (document.body) { " +
            "    var computedStyle = window.getComputedStyle(document.body); " +
            "    testResults.bodyStyle = { " +
            "      backgroundColor: computedStyle.backgroundColor, " +
            "      color: computedStyle.color, " +
            "      display: computedStyle.display, " +
            "      visibility: computedStyle.visibility " +
            "    }; " +
            "  } " +
            "  " +
            "  window._webViewTest = testResults; " +
            "  console.log('WebView Test Results:', testResults); " +
            "  " +
            "  // Test DOM manipulation " +
            "  var testDiv = document.createElement('div'); " +
            "  testDiv.id = 'webview-test-element'; " +
            "  testDiv.innerHTML = 'WebView Test - ' + Date.now(); " +
            "  testDiv.style.position = 'fixed'; " +
            "  testDiv.style.top = '10px'; " +
            "  testDiv.style.right = '10px'; " +
            "  testDiv.style.background = 'yellow'; " +
            "  testDiv.style.padding = '5px'; " +
            "  testDiv.style.zIndex = '9999'; " +
            "  testDiv.style.fontSize = '12px'; " +
            "  document.body.appendChild(testDiv); " +
            "  " +
            "  // Remove test element after 5 seconds " +
            "  setTimeout(function() { " +
            "    var element = document.getElementById('webview-test-element'); " +
            "    if (element) element.remove(); " +
            "  }, 5000); " +
            "  " +
            "  console.log('WebView functionality test completed successfully'); " +
            "} catch(e) { " +
            "  console.error('WebView functionality test failed:', e); " +
            "  window._webViewTest = { error: e.message, timestamp: Date.now() }; " +
            "}";
        
        try {
            browser.getCefBrowser().executeJavaScript(testJs, "", 0);
            log.info("WebView functionality test JavaScript injected");
        } catch (Exception e) {
            log.error("Failed to inject functionality test JavaScript", e);
        }
    }
    
    /**
     * Inject black rectangle detection JavaScript.
     */
    public static void detectBlackRectangle(JBCefBrowser browser) {
        if (!JCEFChecker.isJCEFAvailable() || browser == null || browser.getCefBrowser() == null) {
            log.warn("Cannot detect black rectangle - browser not available");
            return;
        }
        
        String blackRectDetectionJs = 
            "try { " +
            "  console.log('=== Black Rectangle Detection ==='); " +
            "  " +
            "  var detection = { " +
            "    timestamp: Date.now(), " +
            "    bodyExists: !!document.body, " +
            "    bodyVisible: false, " +
            "    bodyBackground: 'unknown', " +
            "    suspiciousBlackFound: false, " +
            "    elements: [] " +
            "  }; " +
            "  " +
            "  if (document.body) { " +
            "    var bodyStyle = window.getComputedStyle(document.body); " +
            "    detection.bodyVisible = document.body.offsetWidth > 0 && document.body.offsetHeight > 0; " +
            "    detection.bodyBackground = bodyStyle.backgroundColor; " +
            "    " +
            "    // Check if body has suspicious black background " +
            "    var bgColor = bodyStyle.backgroundColor; " +
            "    detection.suspiciousBlackFound = bgColor === 'rgb(0, 0, 0)' || bgColor === 'rgba(0, 0, 0, 1)' || bgColor === 'black'; " +
            "    " +
            "    // Check for large black elements " +
            "    var allElements = document.querySelectorAll('*'); " +
            "    for (var i = 0; i < allElements.length; i++) { " +
            "      var el = allElements[i]; " +
            "      var style = window.getComputedStyle(el); " +
            "      var bg = style.backgroundColor; " +
            "      " +
            "      if ((bg === 'rgb(0, 0, 0)' || bg === 'rgba(0, 0, 0, 1)' || bg === 'black') && " +
            "          el.offsetWidth > 100 && el.offsetHeight > 100) { " +
            "        detection.elements.push({ " +
            "          tag: el.tagName, " +
            "          id: el.id, " +
            "          className: el.className, " +
            "          size: el.offsetWidth + 'x' + el.offsetHeight, " +
            "          background: bg " +
            "        }); " +
            "      } " +
            "    } " +
            "  } " +
            "  " +
            "  window._blackRectangleDetection = detection; " +
            "  " +
            "  if (detection.suspiciousBlackFound || detection.elements.length > 0) { " +
            "    console.warn('BLACK RECTANGLE DETECTED:', detection); " +
            "    " +
            "    // Try to fix by forcing a redraw " +
            "    if (document.body) { " +
            "      document.body.style.display = 'none'; " +
            "      setTimeout(function() { " +
            "        document.body.style.display = ''; " +
            "        console.log('Attempted black rectangle fix via display toggle'); " +
            "      }, 100); " +
            "    } " +
            "  } else { " +
            "    console.log('No black rectangle detected:', detection); " +
            "  } " +
            "} catch(e) { " +
            "  console.error('Black rectangle detection failed:', e); " +
            "  window._blackRectangleDetection = { error: e.message, timestamp: Date.now() }; " +
            "}";
        
        try {
            browser.getCefBrowser().executeJavaScript(blackRectDetectionJs, "", 0);
            log.info("Black rectangle detection JavaScript injected");
        } catch (Exception e) {
            log.error("Failed to inject black rectangle detection JavaScript", e);
        }
    }
    
    /**
     * Create a debug panel for WebView information.
     */
    public static JPanel createDebugPanel(JBCefBrowser browser) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("WebView Debug Info"));
        
        JTextArea textArea = new JTextArea(20, 60);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // Initial diagnostics
        String diagnostics = performDiagnostics(browser);
        textArea.setText(diagnostics);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton refreshButton = new JButton("Refresh Diagnostics");
        refreshButton.addActionListener(e -> {
            String newDiagnostics = performDiagnostics(browser);
            textArea.setText(newDiagnostics);
        });
        
        JButton testButton = new JButton("Run Functionality Test");
        testButton.addActionListener(e -> performFunctionalityTest(browser));
        
        JButton blackRectButton = new JButton("Detect Black Rectangle");
        blackRectButton.addActionListener(e -> detectBlackRectangle(browser));
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(testButton);
        buttonPanel.add(blackRectButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Show debug dialog for WebView.
     */
    public static void showDebugDialog(JBCefBrowser browser, Component parent) {
        JDialog dialog = new JDialog();
        dialog.setTitle("WebView Debug Information");
        dialog.setModal(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        dialog.add(createDebugPanel(browser));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
