package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.intellij.testFramework.EdtTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Renders the real Settings → Web search panel to {@code docusaurus/static/img/web-search-settings.png}
 * for the docs. Skipped unless {@code GENERATE_DOCS_SCREENSHOTS=true}:
 *
 * <pre>GENERATE_DOCS_SCREENSHOTS=true ./gradlew test --tests '*WebSearchSettingsScreenshotGenerator'</pre>
 *
 * Only the form is filled in (placeholder keys, masked); nothing is written to settings.
 */
public class WebSearchSettingsScreenshotGenerator extends AbstractLightPlatformTestCase {

    private static final int WIDTH = 900;
    private static final int SCALE = 2;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GENERATE_DOCS_SCREENSHOTS", matches = "true")
    void renderWebSearchSettings() throws Exception {
        EdtTestUtil.runInEdtAndWait(this::renderOnEdt);
    }

    private void renderOnEdt() throws Exception {
        WebSearchProvidersComponent component = new WebSearchProvidersComponent();
        JPanel panel = component.createPanelWithHelp();
        component.getEnableWebSearchCheckbox().setSelected(true);
        component.setSelectedProvider(WebSearchProvider.TAVILY);
        component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("tvly-xxxxxxxxxxxxxxxxxxxx");
        component.getMaxSearchResults().setNumber(5);
        component.getTestSearchQuery().setText("IntelliJ Platform plugin compatibility since-build");

        File out = new File(System.getProperty("user.dir"), "docusaurus/static/img/web-search-settings.png");
        render(panel, out);
    }

    private static void render(JComponent root, File out) throws Exception {
        root.setSize(WIDTH, 10);
        layoutTree(root);
        root.setSize(WIDTH, root.getPreferredSize().height);
        layoutTree(root);

        BufferedImage image = new BufferedImage(WIDTH * SCALE, root.getHeight() * SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(SCALE, SCALE);
            g.setColor(root.getBackground() != null ? root.getBackground() : Color.WHITE);
            g.fillRect(0, 0, WIDTH, root.getHeight());
            root.printAll(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", out);
    }

    private static void layoutTree(Component component) {
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layoutTree(child);
            }
        }
    }
}
