---
sidebar_position: 3
---

# RAG Support

Retrieval-Augmented Generation (RAG) is one of DevoxxGenie's most powerful features, enhancing the LLM's ability to understand and interact with your codebase by automatically finding and incorporating relevant code from your project.

## What is RAG?

Retrieval-Augmented Generation combines retrieval of information with text generation. In the context of DevoxxGenie:

1. **Retrieval**: When you ask a question, DevoxxGenie searches your codebase to find the most relevant files and code snippets
2. **Augmentation**: These relevant code snippets are added to the prompt context
3. **Generation**: The LLM then generates a response informed by this contextual information

This approach dramatically improves the quality of responses about your specific codebase, enabling the LLM to provide more accurate code suggestions, explanations, and fixes.

## Benefits of RAG in DevoxxGenie

- **Better code understanding**: The LLM has access to your specific implementation details
- **Project-aware responses**: Recommendations align with your existing coding style and patterns
- **Contextual debugging**: The LLM can refer to relevant parts of your codebase when helping debug issues
- **More accurate code generation**: Generated code is compatible with your existing architecture
- **Reduced hallucinations**: The LLM has real data to reference instead of guessing about your implementation

## Using RAG in DevoxxGenie

### Enabling RAG

RAG is enabled by default in DevoxxGenie. To configure or adjust RAG settings:

1. Click the settings (gear) icon in the DevoxxGenie window
2. Navigate to the "RAG Settings" section
3. Adjust parameters as needed (see Configuration Options below)

### In Your Workflow

When RAG is enabled, you can:

1. Ask questions directly about your code without explicitly providing context
2. Request explanations of specific components or functionality
3. Ask for code improvements while leveraging existing patterns

For example, queries like these will automatically trigger RAG retrieval:

- "How does the authentication flow work in this project?"
- "Explain the data model for users"
- "Generate a new service method that follows our existing patterns"

### Configuration Options

DevoxxGenie offers several configuration options for RAG:

- **Maximum documents**: Control how many relevant files are included in the context
- **Chunk size**: Adjust how code is broken down for retrieval (smaller chunks for more precise matching)
- **Search strategy**: Choose between different search algorithms
- **Similarity threshold**: Set the minimum relevance score for including a document
- **File type filters**: Include or exclude specific file types from the RAG index

![RAG Settings](/img/rag-settings.png)

## Advanced RAG Features

### Retrieval Methods

DevoxxGenie implements several retrieval methods:

- **Semantic search**: Find contextually similar code using vector embeddings
- **Keyword search**: Match specific terms and patterns
- **Hybrid search**: Combine both approaches for better results

### Custom Index Management

For larger projects, DevoxxGenie allows you to:

- Manually build and update the RAG index
- Select specific packages or directories to include
- Exclude specific files or patterns (e.g., generated code, tests)
- Save and load index configurations

## Examples

### Example 1: Understanding Existing Code

**User Query**: 
```
How does our application handle database connections?
```

**DevoxxGenie Response**:
```
Based on the code in your project, database connections are managed through a connection pool implemented in `DatabaseConnectionManager.java`.

The key components are:

1. Connection pooling using HikariCP:
   ```java
   public class DatabaseConnectionManager {
       private static HikariDataSource dataSource;
       
       static {
           HikariConfig config = new HikariConfig();
           config.setJdbcUrl(DatabaseProperties.getUrl());
           config.setUsername(DatabaseProperties.getUsername());
           config.setPassword(DatabaseProperties.getPassword());
           config.setMaximumPoolSize(10);
           dataSource = new HikariDataSource(config);
       }
       
       public static Connection getConnection() throws SQLException {
           return dataSource.getConnection();
       }
       
       // Other methods...
   }
   ```

2. Connection usage pattern in repositories:
   ```java
   public List<User> findAllUsers() {
       try (Connection conn = DatabaseConnectionManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users")) {
            ResultSet rs = stmt.executeQuery()) {
           // Process results...
       }
   }
   ```

The application follows a proper resource management pattern with try-with-resources to ensure connections are always returned to the pool.
```

### Example 2: Generating Compatible Code

**User Query**: 
```
Generate a new repository method to find users by email domain
```

**DevoxxGenie Response** (after analyzing your codebase):
```
Based on your existing code patterns, here's a method to find users by email domain:

```java
/**
 * Finds all users with email addresses from a specific domain
 * 
 * @param domain The email domain to search for (e.g., "example.com")
 * @return List of User objects matching the domain
 * @throws RepositoryException if a database error occurs
 */
public List<User> findUsersByEmailDomain(String domain) throws RepositoryException {
    List<User> users = new ArrayList<>();
    
    try (Connection conn = DatabaseConnectionManager.getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "SELECT * FROM users WHERE email LIKE ?")) {
        
        stmt.setString(1, "%" + domain);
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                users.add(user);
            }
        }
        
        return users;
    } catch (SQLException e) {
        logger.error("Error finding users by email domain: " + domain, e);
        throw new RepositoryException("Failed to find users by email domain", e);
    }
}
```

This follows your existing pattern of:
1. Using try-with-resources for connection management
2. Following your error handling approach with RepositoryException
3. Using the User mapping pattern consistent with other repository methods
```

## Troubleshooting

### Common Issues

**RAG not returning relevant context:**
- Ensure your search query is specific enough
- Try rebuilding the RAG index
- Check file type filters to ensure relevant files are included

**Performance issues:**
- Reduce the maximum number of documents
- Increase chunk size to reduce the number of chunks
- Exclude large generated files from indexing

**Out of memory errors:**
- Reduce context window size in settings
- Index only specific packages instead of the entire project

## Coming Soon: GraphRAG

In future updates, DevoxxGenie will introduce GraphRAG, an enhanced RAG implementation that:

1. Creates a knowledge graph of your codebase
2. Maps relationships between classes, methods, and other components
3. Provides more accurate retrieval based on code structure
4. Enables more sophisticated reasoning about code architecture

Stay tuned for updates on this exciting feature!
