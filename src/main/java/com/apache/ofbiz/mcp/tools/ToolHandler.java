package com.apache.ofbiz.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public interface ToolHandler {
    String getName();

    Map<String, Object> getDefinition();

    Object execute(Map<String, Object> arguments, String downstreamToken);
}
