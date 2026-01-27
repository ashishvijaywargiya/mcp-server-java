package com.apache.ofbiz.mcp.tools;

import com.apache.ofbiz.mcp.config.AppConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class FindProductByIdTool implements ToolHandler {

    private final AppConfig appConfig;
    private final WebClient webClient;

    public FindProductByIdTool(AppConfig appConfig, WebClient.Builder builder) {
        this.appConfig = appConfig;
        this.webClient = builder.baseUrl(appConfig.getBackendApiBase()).build();
    }

    @Override
    public String getName() {
        return "findProductById";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "name", getName(),
                "description", "Find a product by using its ID.",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "id", Map.of("type", "string", "description", "ID of the product")),
                        "required", java.util.List.of("id")));
    }

    @Override
    public Object execute(Map<String, Object> arguments, String downstreamToken) {
        String id = (String) arguments.get("id");
        try {
            String response = webClient.get()
                    .uri("/rest/products/" + id)
                    .header("Authorization", "Bearer " + appConfig.getBackendAccessToken())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return Map.of("content", java.util.List.of(
                    Map.of("type", "text", "text", "Result: " + response)));
        } catch (Exception e) {
            return Map.of("content", java.util.List.of(
                    Map.of("type", "text", "text", "Error calling OFBiz: " + e.getMessage())));
        }
    }
}
