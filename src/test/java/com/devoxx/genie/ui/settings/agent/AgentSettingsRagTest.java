package com.devoxx.genie.ui.settings.agent;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.service.chromadb.ChromaDBManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentSettingsRagTest extends AbstractLightPlatformTestCase {

    private ChromaDBManager chroma;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        chroma = mock(ChromaDBManager.class);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), ChromaDBManager.class,
                chroma, getTestRootDisposable());
    }

    private static JLabel findWarning(JComponent panel, String message) {
        return UIUtil.findComponentsOfType(panel, JLabel.class).stream()
                .filter(label -> message.equals(label.getText()))
                .findFirst().orElseThrow();
    }

    @Test
    void disabledRagShowsWarningWithoutQueryingChroma() throws Exception {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        Boolean previous = settings.getRagEnabled();
        AgentSettingsConfigurable configurable = new AgentSettingsConfigurable(getProject());
        try {
            settings.setRagEnabled(false);
            EdtTestUtil.runInEdtAndWait(() -> {
                JLabel warning = findWarning(configurable.createComponent(), "RAG is not enabled in settings.");
                assertTrue(warning.getParent().isVisible());
            });
            verifyNoInteractions(chroma);
        } finally {
            EdtTestUtil.runInEdtAndWait(configurable::disposeUIResources);
            settings.setRagEnabled(previous);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void settingsRemainResponsiveWhileCollectionLookupIsPending(boolean malformedResponse) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(chroma.listCollections()).thenAnswer(invocation -> {
            assertFalse("ChromaDB lookup ran on the EDT", SwingUtilities.isEventDispatchThread());
            started.countDown();
            assertTrue("Settings blocked on the ChromaDB lookup", release.await(10, SECONDS));
            if (malformedResponse) {
                throw new IllegalStateException("Expected a collection array but received an object");
            }
            return List.of();
        });

        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        Boolean previous = settings.getRagEnabled();
        AgentSettingsConfigurable configurable = new AgentSettingsConfigurable(getProject());
        try {
            settings.setRagEnabled(true);
            CountDownLatch shown = new CountDownLatch(1);
            EdtTestUtil.runInEdtAndWait(() -> {
                JLabel warning = findWarning(configurable.createComponent(), "Project has not been indexed yet in RAG.");
                assertFalse(warning.getParent().isVisible());
                warning.getParent().addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentShown(ComponentEvent e) {
                        shown.countDown();
                    }
                });
            });
            assertTrue("Expected a RAG lookup", started.await(10, SECONDS));
            // A second EDT event must run before the fake database sends its response.
            EdtTestUtil.runInEdtAndWait(release::countDown);
            assertTrue("Missing index warning was not shown", shown.await(10, SECONDS));
        } finally {
            release.countDown();
            EdtTestUtil.runInEdtAndWait(configurable::disposeUIResources);
            settings.setRagEnabled(previous);
        }
    }
}
