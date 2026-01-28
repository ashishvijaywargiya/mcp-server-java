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
        try {
            emitter.send(SseEmitter.event().name("endpoint").data("/mcp")); // Send endpoint URI
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handlePost(@RequestBody JsonRpcRequest request) {
        if ("initialize".equals(request.getMethod())) {
            Map<String, Object> capabilities = Map.of(
                    "tools", Map.of("listChanged", true),
                    "resources", Map.of("listChanged", false),
                    "prompts", Map.of("listChanged", false),
                    "logging", Map.of());

            Map<String, Object> result = Map.of(
                    "protocolVersion", "2024-11-05", // Using a recent stable version
                    "capabilities", capabilities,
                    "serverInfo", Map.of(
                            "name", "ashish-mcp-server-java",
                            "version", "0.0.1"));
            return JsonRpcResponse.success(request.getId(), result);
        } else if ("notifications/initialized".equals(request.getMethod())) {
            // No response expected for notifications, but we can return success purely for
            // RPC handling if needed,
            // though usually notifications don't get responses.
            // However, since this is a @PostMapping returning JsonRpcResponse, we'll return
            // a result to avoid 204/Empty issues if the client waits for ACK (though
            // strictly it shouldn't).
            // Better behavior for notification: strictly return null or nothing if the
            // framework allows,
            // but for simple RPC wrapper, returning a success(null) is often safe or
            // ignored.
            // Let's check JsonRpcResponse structure.
            return null; // Spring might return empty body 200 OK which is fine for notification
        } else if ("tools/list".equals(request.getMethod())) {
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
        } else if ("ping".equals(request.getMethod())) {
            return JsonRpcResponse.success(request.getId(), Map.of());
        }
        return JsonRpcResponse.error(request.getId(), -32601, "Method not found");
    }
}
