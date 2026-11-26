package com.devoxx.genie.service.conversations;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.trace.StoredTraceEvent;
import com.devoxx.genie.service.trace.TraceEvent;
import com.devoxx.genie.service.trace.TraceEvent.ToolCall;
import com.devoxx.genie.service.trace.TraceScanStatus;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** task-279: storage of agent traces in {@code trace_events}. */
class ConversationTraceStorageTest {

    @TempDir
    Path tempDir;

    private Path db;
    private ConversationStorageService storage;
    private Project project;

    @BeforeEach
    void setUp() {
        db = tempDir.resolve("conversations.db");
        storage = new ConversationStorageService(db.toString());
        project = mock(Project.class);
        when(project.getLocationHash()).thenReturn("hash-1");
    }

    private static Conversation conversation(String id) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setTitle("t-" + id);
        c.setTimestamp("2026-11-25T10:00:00." + id);
        c.setModelName("m");
        c.setLlmProvider("p");
        c.setApiKeyUsed(false);
        c.setInputCost(0L);
        c.setOutputCost(0L);
        c.setContextWindow(0);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(true, "q", null));
        messages.add(new ChatMessage(false, "a", null));
        c.setMessages(messages);
        return c;
    }

    private static List<TraceEvent> agentTurn() {
        return List.of(
                TraceEvent.user("Fix the test", null, 1L),
                TraceEvent.assistant("", "I should read the file",
                        List.of(new ToolCall("call_1", "read_file", "{\"path\":\"a.java\"}")), "gpt-x", null, 2L),
                TraceEvent.tool("call_1", "read_file", "class A {}", false, null, 3L),
                TraceEvent.assistant("Fixed it.", null, List.of(), "gpt-x", null, 4L));
    }

    @Test
    void addTraceTurn_roundTripsAllFieldsInOrder() {
        storage.addConversation(project, conversation("1"));
        storage.addTraceTurn("1", "turn-a", agentTurn(), TraceScanStatus.CLEAN);

        List<StoredTraceEvent> rows = storage.getTraceEvents("1");

        assertThat(rows).extracting(StoredTraceEvent::event).containsExactlyElementsOf(agentTurn());
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.turnId()).isEqualTo("turn-a");
            assertThat(r.conversationId()).isEqualTo("1");
            assertThat(r.scanStatus()).isEqualTo(TraceScanStatus.CLEAN);
        });
    }

    @Test
    void savingTheConversationAgain_keepsItsTraceRows() {
        // addConversation rewrites chat_messages on every save; trace_events must survive that.
        storage.addConversation(project, conversation("1"));
        storage.addTraceTurn("1", "turn-a", agentTurn(), TraceScanStatus.NOT_SCANNED);
        storage.addConversation(project, conversation("1"));

        assertThat(storage.getTraceEvents("1")).hasSize(4);
    }

    @Test
    void removeConversation_deletesItsTraceRowsOnly() {
        storage.addConversation(project, conversation("1"));
        storage.addConversation(project, conversation("2"));
        storage.addTraceTurn("1", "turn-a", agentTurn(), TraceScanStatus.CLEAN);
        storage.addTraceTurn("2", "turn-b", agentTurn(), TraceScanStatus.CLEAN);

        storage.removeConversation(project, conversation("1"));

        assertThat(storage.getTraceEvents("1")).isEmpty();
        assertThat(storage.getTraceEvents("2")).hasSize(4);
    }

    @Test
    void clearAllConversations_deletesTheProjectsTraceRows() {
        Project other = mock(Project.class);
        when(other.getLocationHash()).thenReturn("hash-2");
        storage.addConversation(project, conversation("1"));
        storage.addConversation(other, conversation("2"));
        storage.addTraceTurn("1", "turn-a", agentTurn(), TraceScanStatus.CLEAN);
        storage.addTraceTurn("2", "turn-b", agentTurn(), TraceScanStatus.CLEAN);

        storage.clearAllConversations(project);

        assertThat(storage.getTraceEvents("1")).isEmpty();
        assertThat(storage.getTraceEvents("2")).hasSize(4);
    }

    @Test
    void severalTurns_areReadBackInWriteOrder() {
        storage.addConversation(project, conversation("1"));
        storage.addTraceTurn("1", "turn-a", agentTurn(), TraceScanStatus.CLEAN);
        storage.addTraceTurn("1", "turn-b", List.of(TraceEvent.user("next", null, 5L)), TraceScanStatus.REDACTED);

        List<StoredTraceEvent> rows = storage.getTraceEvents("1");

        assertThat(rows).extracting(StoredTraceEvent::turnId)
                .containsExactly("turn-a", "turn-a", "turn-a", "turn-a", "turn-b");
        assertThat(rows.get(4).scanStatus()).isEqualTo(TraceScanStatus.REDACTED);
    }

    @Test
    void subAgentAndErrorFields_roundTrip() {
        storage.addConversation(project, conversation("1"));
        TraceEvent failed = TraceEvent.tool("c9", "run_command", "Error: boom", true, "sub-agent-1", 7L);
        storage.addTraceTurn("1", "turn-a", List.of(failed), TraceScanStatus.NOT_SCANNED);

        assertThat(storage.getTraceEvents("1")).singleElement()
                .satisfies(r -> assertThat(r.event()).isEqualTo(failed));
    }

    @Test
    void hasTraceEvents_reflectsStoredRows() {
        storage.addConversation(project, conversation("1"));
        assertThat(storage.hasTraceEvents("1")).isFalse();
        storage.addTraceTurn("1", "turn-a", agentTurn(), TraceScanStatus.CLEAN);
        assertThat(storage.hasTraceEvents("1")).isTrue();
        assertThat(storage.hasTraceEvents("2")).isFalse();
    }

    @Test
    void conversationIdsWithTraces_listsOnlyTracedConversations() {
        storage.addConversation(project, conversation("1"));
        storage.addConversation(project, conversation("2"));
        assertThat(storage.getConversationIdsWithTraces()).isEmpty();

        storage.addTraceTurn("1", "turn-a", agentTurn(), TraceScanStatus.CLEAN);
        storage.addTraceTurn("1", "turn-b", agentTurn(), TraceScanStatus.CLEAN);

        assertThat(storage.getConversationIdsWithTraces()).containsExactly("1");
    }

    @Test
    void emptyTurn_writesNothing() {
        storage.addTraceTurn("1", "turn-a", List.of(), TraceScanStatus.CLEAN);
        assertThat(storage.getTraceEvents("1")).isEmpty();
    }

    @Test
    void existingDatabaseWithoutTraceTable_isUpgradedAndKeepsItsData() throws Exception {
        Path legacy = tempDir.resolve("legacy.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + legacy);
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE conversations (id TEXT PRIMARY KEY, projectHash TEXT, timestamp TEXT, title TEXT, "
                    + "llmProvider TEXT, modelName TEXT, apiKeyUsed INTEGER, inputCost INTEGER, outputCost INTEGER, "
                    + "contextWindow INTEGER, executionTimeMs INTEGER)");
            s.execute("CREATE TABLE chat_messages (id INTEGER PRIMARY KEY AUTOINCREMENT, conversationId TEXT, "
                    + "content TEXT, isUser INTEGER)");
            s.execute("INSERT INTO conversations (id, projectHash, title) VALUES ('old', 'hash-1', 'Old chat')");
            s.execute("INSERT INTO chat_messages (conversationId, content, isUser) VALUES ('old', 'hello', 1)");
        }

        ConversationStorageService upgraded = new ConversationStorageService(legacy.toString());

        assertThat(upgraded.getConversations(project)).singleElement()
                .satisfies(c -> assertThat(c.getMessages()).extracting(ChatMessage::getContent).containsExactly("hello"));
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + legacy);
             ResultSet rs = c.getMetaData().getTables(null, null, "trace_events", null)) {
            assertThat(rs.next()).isTrue();
        }
        assertThat(upgraded.getTraceEvents("old")).isEmpty();
    }
}
