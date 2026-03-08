package com.devoxx.genie.ui.window;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.util.messages.MessageBusConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversationTabRegistryTest {

    private ConversationTabRegistry registry;

    @Mock private Content content1;
    @Mock private Content content2;
    @Mock private DevoxxGenieToolWindowContent twc1;
    @Mock private DevoxxGenieToolWindowContent twc2;
    @Mock private Project project;
    @Mock private MessageBusConnection connection1;
    @Mock private MessageBusConnection connection2;

    @BeforeEach
    void setUp() {
        registry = new ConversationTabRegistry();
    }

    @Nested
    class Register {

        @Test
        void shouldStoreContentMapping() {
            registry.register(content1, twc1);

            assertThat(registry.getToolWindowContent(content1)).isSameAs(twc1);
        }

        @Test
        void shouldSupportMultipleRegistrations() {
            registry.register(content1, twc1);
            registry.register(content2, twc2);

            assertThat(registry.getToolWindowContent(content1)).isSameAs(twc1);
            assertThat(registry.getToolWindowContent(content2)).isSameAs(twc2);
        }

        @Test
        void shouldReturnNullForUnregisteredContent() {
            assertThat(registry.getToolWindowContent(content1)).isNull();
        }
    }

    @Nested
    class RegisterConnection {

        @Test
        void shouldStoreConnectionForContent() {
            registry.register(content1, twc1);
            registry.registerConnection(content1, connection1);

            // Connection is stored; verified via unregister disconnecting it
            registry.unregister(content1);
            verify(connection1).disconnect();
        }
    }

    @Nested
    class Unregister {

        @Test
        void shouldRemoveContentMapping() {
            registry.register(content1, twc1);
            registry.unregister(content1);

            assertThat(registry.getToolWindowContent(content1)).isNull();
        }

        @Test
        void shouldDisconnectMessageBusConnection() {
            registry.register(content1, twc1);
            registry.registerConnection(content1, connection1);

            registry.unregister(content1);

            verify(connection1).disconnect();
        }

        @Test
        void shouldNotFailWhenNoConnectionRegistered() {
            registry.register(content1, twc1);
            // No connection registered — should not throw
            registry.unregister(content1);

            assertThat(registry.getToolWindowContent(content1)).isNull();
        }

        @Test
        void shouldNotAffectOtherTabs() {
            registry.register(content1, twc1);
            registry.register(content2, twc2);
            registry.registerConnection(content1, connection1);
            registry.registerConnection(content2, connection2);

            registry.unregister(content1);

            assertThat(registry.getToolWindowContent(content1)).isNull();
            assertThat(registry.getToolWindowContent(content2)).isSameAs(twc2);
            verify(connection1).disconnect();
            verify(connection2, never()).disconnect();
        }
    }

    @Nested
    class GetContentsForProject {

        @Test
        void shouldReturnOnlyContentsForMatchingProject() {
            Project otherProject = mock(Project.class);
            when(twc1.getProject()).thenReturn(project);
            when(twc2.getProject()).thenReturn(otherProject);

            registry.register(content1, twc1);
            registry.register(content2, twc2);

            List<DevoxxGenieToolWindowContent> result = registry.getContentsForProject(project);

            assertThat(result).containsExactly(twc1);
        }

        @Test
        void shouldReturnEmptyListWhenNoTabsForProject() {
            List<DevoxxGenieToolWindowContent> result = registry.getContentsForProject(project);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnMultipleContentsForSameProject() {
            when(twc1.getProject()).thenReturn(project);
            when(twc2.getProject()).thenReturn(project);

            registry.register(content1, twc1);
            registry.register(content2, twc2);

            List<DevoxxGenieToolWindowContent> result = registry.getContentsForProject(project);

            assertThat(result).containsExactlyInAnyOrder(twc1, twc2);
        }
    }
}
