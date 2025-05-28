package com.devoxx.genie.service.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.ToolSpecificationHelper;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolSpecificationHelperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testJsonNodeToJsonSchemaElement_withNullNode() {
        JsonSchemaElement result = ToolSpecificationHelper.jsonNodeToJsonSchemaElement(null);
        assertNotNull(result);
        assertTrue(result instanceof JsonObjectSchema);
    }

    @Test
    void testJsonNodeToJsonSchemaElement_withoutTypeField() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("description", "A test schema");
        ObjectNode properties = objectMapper.createObjectNode();
        properties.put("name", objectMapper.createObjectNode().put("type", "string"));
        node.set("properties", properties);
        
        JsonSchemaElement result = ToolSpecificationHelper.jsonNodeToJsonSchemaElement(node);
        assertNotNull(result);
        assertTrue(result instanceof JsonObjectSchema);
        
        JsonObjectSchema objectSchema = (JsonObjectSchema) result;
        assertEquals("A test schema", objectSchema.description());
    }

    @Test
    void testJsonNodeToJsonSchemaElement_withTypeField() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "object");
        node.put("description", "A test schema");
        
        JsonSchemaElement result = ToolSpecificationHelper.jsonNodeToJsonSchemaElement(node);
        assertNotNull(result);
        assertTrue(result instanceof JsonObjectSchema);
    }

    @Test
    void testToolSpecificationListFromMcpResponse() {
        ArrayNode toolsArray = objectMapper.createArrayNode();
        
        // Add a tool with no type in inputSchema
        ObjectNode tool1 = objectMapper.createObjectNode();
        tool1.put("name", "testTool1");
        tool1.put("description", "A test tool");
        ObjectNode inputSchema1 = objectMapper.createObjectNode();
        inputSchema1.put("description", "Tool parameters");
        tool1.set("inputSchema", inputSchema1);
        toolsArray.add(tool1);
        
        // Add a tool with type in inputSchema
        ObjectNode tool2 = objectMapper.createObjectNode();
        tool2.put("name", "testTool2");
        ObjectNode inputSchema2 = objectMapper.createObjectNode();
        inputSchema2.put("type", "object");
        tool2.set("inputSchema", inputSchema2);
        toolsArray.add(tool2);
        
        List<ToolSpecification> result = ToolSpecificationHelper.toolSpecificationListFromMcpResponse(toolsArray);
        
        assertEquals(2, result.size());
        assertEquals("testTool1", result.get(0).name());
        assertEquals("A test tool", result.get(0).description());
        assertEquals("testTool2", result.get(1).name());
    }
}