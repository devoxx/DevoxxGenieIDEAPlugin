package com.devoxx.genie.model.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPMessage {
    private MCPType type;

    // STDIO transport properties
    private String content;

}
