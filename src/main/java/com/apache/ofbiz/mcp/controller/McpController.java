package com.apache.ofbiz.mcp.controller;

import com.apache.ofbiz.mcp.model.JsonRpcRequest;
import com.apache.ofbiz.mcp.model.JsonRpcResponse;
import com.apache.ofbiz.mcp.tools.ToolHandler;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private final Map<String, ToolHandler> tools;

    public McpController(List<ToolHandler> toolHandlers) {
        this.tools = new ConcurrentHashMap<>();
        toolHandlers.forEach(t -> tools.put(t.getName(), t));
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSse() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        // Implement connection init logic if needed
        return emitter;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handlePost(@RequestBody JsonRpcRequest request) {
        if ("tools/list".equals(request.getMethod())) {
            var toolDefs = tools.values().stream().map(ToolHandler::getDefinition).collect(Collectors.toList());
            return JsonRpcResponse.success(request.getId(), Map.of("tools", toolDefs));
        } else if ("tools/call".equals(request.getMethod())) {
            String toolName = (String) request.getParams().get("name");
            Map<String, Object> args = (Map<String, Object>) request.getParams().get("arguments");

            if (tools.containsKey(toolName)) {
                try {
                    Object result = tools.get(toolName).execute(args, null); // Pass token if extracted
                    return JsonRpcResponse.success(request.getId(), result);
                } catch (Exception e) {
                    return JsonRpcResponse.error(request.getId(), -32000, e.getMessage());
                }
            } else {
                return JsonRpcResponse.error(request.getId(), -32601, "Method not found");
            }
        }
        return JsonRpcResponse.error(request.getId(), -32601, "Method not found");
    }
}
