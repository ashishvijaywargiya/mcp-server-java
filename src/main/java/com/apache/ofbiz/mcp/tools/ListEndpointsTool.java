package com.apache.ofbiz.mcp.tools;

import com.apache.ofbiz.mcp.config.AppConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Component
public class ListEndpointsTool implements ToolHandler {

    private final WebClient webClient;
    private static final List<String> COMMON_PATHS = List.of(
            "/rest/openapi.json",
            "/rest/api.json",
            "/openapi.json",
            "/rest/v1/openapi.json");

    public ListEndpointsTool(AppConfig appConfig, WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl(appConfig.getBackendApiBase())
                .build();
    }

    @Override
    public String getName() {
        return "listEndpoints";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "name", getName(),
                "description",
                "Lists publicly available REST API endpoints from OFBiz by checking common OpenAPI paths.",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of()));
    }

    @Override
    public Object execute(Map<String, Object> arguments, String downstreamToken) {
        List<String> foundEndpoints = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String path : COMMON_PATHS) {
            try {
                // We just check if it exists and returns JSON
                String response = webClient.get()
                        .uri(path)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (response != null && !response.isEmpty()) {
                    foundEndpoints.add("Found API definition at: " + path);
                    // In a real scenario, we might parse the JSON to list specific paths
                }
            } catch (WebClientResponseException.NotFound e) {
                // Ignore 404
            } catch (Exception e) {
                errors.add("Error checking " + path + ": " + e.getMessage());
            }
        }

        if (foundEndpoints.isEmpty()) {
            return Map.of("content", List.of(
                    Map.of("type", "text", "text",
                            "No API definitions found at common paths: " + COMMON_PATHS + ". Errors: " + errors)));
        }

        return Map.of("content", List.of(
                Map.of("type", "text", "text", String.join("\n", foundEndpoints))));
    }
}
