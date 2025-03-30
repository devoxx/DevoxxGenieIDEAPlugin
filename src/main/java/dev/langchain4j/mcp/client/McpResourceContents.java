package dev.langchain4j.mcp.client;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

/**
 * A holder for either a 'McpTextResourceContents' or 'McpBlobResourceContents'
 * object from the MCP protocol schema.
 */
@JsonTypeInfo(use = DEDUCTION)
@JsonSubTypes({@JsonSubTypes.Type(McpTextResourceContents.class), @JsonSubTypes.Type(McpBlobResourceContents.class)})
public sealed interface McpResourceContents permits McpTextResourceContents, McpBlobResourceContents {

    Type type();

    enum Type {
        TEXT,
        BLOB
    }
}
