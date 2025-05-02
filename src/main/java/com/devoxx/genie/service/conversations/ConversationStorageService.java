package com.devoxx.genie.service.conversations;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Instead of using the IntelliJ State API, we use a separate database to store conversations.
 * SQLite is a lightweight database that can be used for small applications like this.
 * The conversations.db is stored for mac in
 * /Users/[username]/Library/Caches/JetBrains/IntelliJIdea2024.3/DevoxxGenie/conversations.db
 * You can connect to the SQLite db using IDEA's Database tool window.
 */
@Slf4j
public class ConversationStorageService {
    
    private final String dbPath;
    private static final long MAX_DB_SIZE_BYTES = 50 * 1024 * 1024;  // 50 MB threshold
    private static final int DELETE_COUNT = 10; // Delete 10 oldest conversations

    public ConversationStorageService() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }

        this.dbPath = Path.of(PathManager.getSystemPath(), "DevoxxGenie", "conversations.db").toString();
        try {
            Files.createDirectories(Path.of(dbPath).getParent());
            log.info("Database directory created at " + dbPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create database directory", e);
        }
        createTableIfNotExists();
        migrateDatabase();
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
                            isUser INTEGER,
                            FOREIGN KEY (conversationId) REFERENCES conversations(id)
                        )
                    """);

            // Add indices for better performance
            statement.execute("CREATE INDEX IF NOT EXISTS idx_conversations_project ON conversations(projectHash)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_messages_conversation ON chat_messages(conversationId)");
        } catch (SQLException e) {
            log.error("Error creating table", e);
            throw new RuntimeException("Error creating table", e);
        }
    }

    public void addConversation(@NotNull Project project, @NotNull Conversation conversation) {
        // Cleanup old conversations asynchronously if the DB size exceeds the threshold
        CompletableFuture.runAsync(() -> {
            try {
                Path dbFile = Path.of(dbPath);
                if (Files.size(dbFile) > MAX_DB_SIZE_BYTES) {
                    cleanupOldConversations();
                }
            } catch (IOException e) {
                log.error("Error checking DB size asynchronously", e);
            }
        });

        // Add the conversation
        try (Connection connection = getConnection()) {
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
                    // Handle nulls for boolean values
                    ps.setInt(7, conversation.getApiKeyUsed() != null && conversation.getApiKeyUsed() ? 1 : 0);
                    ps.setLong(8, conversation.getInputCost() == null ? 0 : conversation.getInputCost());
                    ps.setLong(9, conversation.getOutputCost() == null ? 0 : conversation.getOutputCost());
                    ps.setInt(10, conversation.getContextWindow() == null ? 0 : conversation.getContextWindow());
                    ps.setInt(11, (int) (conversation.getExecutionTimeMs() > 0 ? conversation.getExecutionTimeMs() : 0));
                    ps.executeUpdate();
                }

                // Insert chat messages
                try (PreparedStatement msgPs = connection.prepareStatement(
                        "INSERT INTO chat_messages (conversationId, content, isUser) VALUES (?, ?, ?)")) {
                    for (ChatMessage message : conversation.getMessages()) {
                        msgPs.setString(1, conversation.getId());
                        msgPs.setString(2, message.getContent());
                        msgPs.setInt(3, message.isUser() ? 1 : 0); // Store isUser as INTEGER (1=true, 0=false)
                        msgPs.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                log.error("Error adding conversation", e);
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
                                
                                // Check if the isUser column exists in the result set
                                try {
                                    int isUserFlag = msgRs.getInt("isUser");
                                    message.setUser(isUserFlag == 1);
                                    log.debug("Set isUser flag to {} for message", isUserFlag == 1);
                                } catch (SQLException e) {
                                    // Column doesn't exist in older database versions, default to alternating pattern
                                    message.setUser(messages.size() % 2 == 0);
                                    log.debug("isUser column not found, defaulting to alternating pattern");
                                }
                                
                                messages.add(message);
                            }
                        }
                    }
                    conversation.setMessages(messages);
                    conversations.add(conversation);
                }
            }
        } catch (SQLException e) {
            log.error("Error getting conversations", e);
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
                    log.info("Deleted {} messages for conversation {}", messagesDeleted, conversation.getId());
                    if (messagesDeleted == 0) {
                        log.warn("No messages found for conversation {}", conversation.getId());
                    }
                }

                // Then delete the conversation
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM conversations WHERE id = ? AND projectHash = ?")) {
                    ps.setString(1, conversation.getId());
                    ps.setString(2, project.getLocationHash());
                    int conversationsDeleted = ps.executeUpdate();
                    log.info("Deleted " + conversationsDeleted + " conversations with ID " + conversation.getId());
                    if (conversationsDeleted == 0) {
                        log.warn("No conversation found with ID " + conversation.getId() + " and project hash " + project.getLocationHash());
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                log.error("Error removing conversation", e);
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
                log.error("Error clearing conversations", e);
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing conversations", e);
        }
    }

    private void cleanupOldConversations() {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Delete the oldest DELETE_COUNT conversations based on the timestamp
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM conversations WHERE id IN (" +
                                "SELECT id FROM conversations ORDER BY timestamp ASC LIMIT ?)")) {
                    ps.setInt(1, DELETE_COUNT);
                    int deleted = ps.executeUpdate();
                    log.info("Deleted {} old conversations to free up space", deleted);
                }
                // Also delete associated messages
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM chat_messages WHERE conversationId NOT IN (SELECT id FROM conversations)")) {
                    int msgDeleted = ps.executeUpdate();
                    log.info("Deleted {} orphaned chat messages", msgDeleted);
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing conversations", e);
        }
    }
    
    /**
     * Migrate database to newer schema if needed.
     * This method handles adding new columns and populating existing records
     * with appropriate values to ensure backward compatibility.
     */
    private void migrateDatabase() {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            
            try {
                // Check if isUser column exists in chat_messages table
                boolean isUserColumnExists = false;
                try (Statement statement = connection.createStatement();
                     ResultSet rs = statement.executeQuery("PRAGMA table_info(chat_messages)")) {
                    
                    while (rs.next()) {
                        String columnName = rs.getString("name");
                        if ("isUser".equals(columnName)) {
                            isUserColumnExists = true;
                            log.info("isUser column already exists in chat_messages table");
                            break;
                        }
                    }
                }
                
                if (!isUserColumnExists) {
                    log.info("Adding isUser column to chat_messages table");
                    
                    // Add the isUser column to the table
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE chat_messages ADD COLUMN isUser INTEGER DEFAULT 0");
                    }
                    
                    // Process all conversations to set isUser flag correctly
                    try (Statement statement = connection.createStatement();
                         ResultSet conversationRs = statement.executeQuery("SELECT id FROM conversations")) {
                        
                        while (conversationRs.next()) {
                            String conversationId = conversationRs.getString("id");
                            
                            // Get messages for this conversation
                            try (PreparedStatement msgPs = connection.prepareStatement(
                                 "SELECT id FROM chat_messages WHERE conversationId = ? ORDER BY id")) {
                                msgPs.setString(1, conversationId);
                                ResultSet msgRs = msgPs.executeQuery();
                                
                                // Set every even-indexed message as user (isUser=1)
                                // and every odd-indexed message as AI (isUser=0)
                                int messageIndex = 0;
                                while (msgRs.next()) {
                                    int messageId = msgRs.getInt("id");
                                    boolean isUser = messageIndex % 2 == 0; // Even index = user message
                                    
                                    try (PreparedStatement updatePs = connection.prepareStatement(
                                         "UPDATE chat_messages SET isUser = ? WHERE id = ?")) {
                                        updatePs.setInt(1, isUser ? 1 : 0);
                                        updatePs.setInt(2, messageId);
                                        updatePs.executeUpdate();
                                    }
                                    
                                    messageIndex++;
                                }
                                
                                log.debug("Updated {} messages for conversation {}", messageIndex, conversationId);
                            }
                        }
                    }
                    
                    log.info("Database migration completed successfully");
                }
                
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                log.error("Error during database migration", e);
                // Don't throw exception to allow application to continue with fallback behavior
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Failed to connect to database for migration", e);
            // Don't throw exception to allow application to continue with fallback behavior
        }
    }
}
