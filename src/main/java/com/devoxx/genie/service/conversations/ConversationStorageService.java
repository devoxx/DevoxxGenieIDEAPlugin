package com.devoxx.genie.service.conversations;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Instead of using the IntelliJ State API, we use a separate database to store conversations.
 * SQLite is a lightweight database that can be used for small applications like this.
 * The conversations.db is stored for mac in
 * /Users/[username]/Library/Caches/JetBrains/IntelliJIdea2024.3/DevoxxGenie/conversations.db
 * You can connect to the SQLite db using IDEA's Database tool window.
 */
public class ConversationStorageService {
    private static final Logger LOG = Logger.getInstance(ConversationStorageService.class);

    private final String dbPath;

    public ConversationStorageService() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }

        this.dbPath = Path.of(PathManager.getSystemPath(), "DevoxxGenie", "conversations.db").toString();
        try {
            Files.createDirectories(Path.of(dbPath).getParent());
            LOG.info("Database directory created at " + dbPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create database directory", e);
        }
        createTableIfNotExists();
    }

    public static @NotNull ConversationStorageService getInstance() {
        return new ConversationStorageService();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    private void createTableIfNotExists() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS conversations (
                            id TEXT PRIMARY KEY,
                            projectHash TEXT,
                            timestamp TEXT,
                            title TEXT,
                            llmProvider TEXT,
                            modelName TEXT,
                            apiKeyUsed INTEGER,
                            inputCost INTEGER,
                            outputCost INTEGER,
                            contextWindow INTEGER,
                            executionTimeMs INTEGER
                        )
                    """);

            // SQLite uses INTEGER PRIMARY KEY for autoincrement
            statement.execute("""
                        CREATE TABLE IF NOT EXISTS chat_messages (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            conversationId TEXT,
                            content TEXT,
                            FOREIGN KEY (conversationId) REFERENCES conversations(id)
                        )
                    """);

            // Add indices for better performance
            statement.execute("CREATE INDEX IF NOT EXISTS idx_conversations_project ON conversations(projectHash)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_messages_conversation ON chat_messages(conversationId)");
        } catch (SQLException e) {
            LOG.error("Error creating table", e);
            throw new RuntimeException("Error creating table", e);
        }
    }

    public void addConversation(@NotNull Project project, @NotNull Conversation conversation) {
        try (Connection connection = getConnection()) {
            // Start transaction
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement(
                        """
                                INSERT INTO conversations 
                                (id, projectHash, timestamp, title, llmProvider, modelName, 
                                 apiKeyUsed, inputCost, outputCost, contextWindow, executionTimeMs)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                """)) {
                    ps.setString(1, conversation.getId());
                    ps.setString(2, project.getLocationHash());
                    ps.setString(3, conversation.getTimestamp());
                    ps.setString(4, conversation.getTitle());
                    ps.setString(5, conversation.getLlmProvider());
                    ps.setString(6, conversation.getModelName());
                    ps.setInt(7, conversation.getApiKeyUsed() ? 1 : 0); // SQLite uses INTEGER for boolean
                    ps.setLong(8, conversation.getInputCost() == null ? 0 : conversation.getInputCost());
                    ps.setLong(9, conversation.getOutputCost() == null ? 0 : conversation.getOutputCost());
                    ps.setInt(10, conversation.getContextWindow() == null ? 0 : conversation.getContextWindow());
                    ps.setInt(11, (int) conversation.getExecutionTimeMs());
                    ps.executeUpdate();
                }

                // Insert chat messages
                try (PreparedStatement msgPs = connection.prepareStatement(
                        "INSERT INTO chat_messages (conversationId, content) VALUES (?, ?)")) {
                    for (ChatMessage message : conversation.getMessages()) {
                        msgPs.setString(1, conversation.getId());
                        msgPs.setString(2, message.getContent());
                        msgPs.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                LOG.error("Error adding conversation", e);
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error adding conversation", e);
        }
    }

    @NotNull
    public List<Conversation> getConversations(@NotNull Project project) {
        List<Conversation> conversations = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT * FROM conversations WHERE projectHash = ?")) {
            ps.setString(1, project.getLocationHash());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Conversation conversation = new Conversation();
                    conversation.setId(rs.getString("id"));
                    conversation.setTimestamp(rs.getString("timestamp"));
                    conversation.setTitle(rs.getString("title"));
                    conversation.setLlmProvider(rs.getString("llmProvider"));
                    conversation.setModelName(rs.getString("modelName"));
                    conversation.setApiKeyUsed(rs.getInt("apiKeyUsed") == 1); // Convert INTEGER to boolean
                    conversation.setInputCost(rs.getLong("inputCost"));
                    conversation.setOutputCost(rs.getLong("outputCost"));
                    conversation.setContextWindow(rs.getInt("contextWindow"));
                    conversation.setExecutionTimeMs(rs.getInt("executionTimeMs"));

                    // Retrieve chat messages for the conversation
                    List<ChatMessage> messages = new ArrayList<>();
                    try (PreparedStatement msgPs = connection.prepareStatement(
                            "SELECT * FROM chat_messages WHERE conversationId = ? ORDER BY id")) {
                        msgPs.setString(1, conversation.getId());
                        try (ResultSet msgRs = msgPs.executeQuery()) {
                            while (msgRs.next()) {
                                ChatMessage message = new ChatMessage();
                                message.setContent(msgRs.getString("content"));
                                messages.add(message);
                            }
                        }
                    }
                    conversation.setMessages(messages);
                    conversations.add(conversation);
                }
            }
        } catch (SQLException e) {
            LOG.error("Error getting conversations", e);
            throw new RuntimeException("Error getting conversations", e);
        }
        return conversations;
    }

    public void removeConversation(@NotNull Project project, @NotNull Conversation conversation) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Delete messages first due to foreign key constraint
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM chat_messages WHERE conversationId = ?")) {
                    ps.setString(1, conversation.getId());
                    int messagesDeleted = ps.executeUpdate();
                    LOG.info("Deleted " + messagesDeleted + " messages for conversation " + conversation.getId());
                    if (messagesDeleted == 0) {
                        LOG.warn("No messages found for conversation " + conversation.getId());
                    }
                }

                // Then delete the conversation
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM conversations WHERE id = ? AND projectHash = ?")) {
                    ps.setString(1, conversation.getId());
                    ps.setString(2, project.getLocationHash());
                    int conversationsDeleted = ps.executeUpdate();
                    LOG.info("Deleted " + conversationsDeleted + " conversations with ID " + conversation.getId());
                    if (conversationsDeleted == 0) {
                        LOG.warn("No conversation found with ID " + conversation.getId() + " and project hash " + project.getLocationHash());
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                LOG.error("Error removing conversation", e);
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error removing conversation", e);
        }
    }

    public void clearAllConversations(@NotNull Project project) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Delete all messages for conversations in this project
                try (PreparedStatement ps = connection.prepareStatement(
                        """
                                DELETE FROM chat_messages 
                                WHERE conversationId IN (
                                    SELECT id FROM conversations WHERE projectHash = ?
                                )
                                """)) {
                    ps.setString(1, project.getLocationHash());
                    ps.executeUpdate();
                }

                // Then delete all conversations
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM conversations WHERE projectHash = ?")) {
                    ps.setString(1, project.getLocationHash());
                    ps.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                LOG.error("Error clearing conversations", e);
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing conversations", e);
        }
    }
}
