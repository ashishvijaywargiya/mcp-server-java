package com.apache.ofbiz.mcp.tools;

import com.apache.ofbiz.mcp.config.AppConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CreateBatchExamplesTool implements ToolHandler {

    private final AppConfig appConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateBatchExamplesTool(AppConfig appConfig, WebClient.Builder builder) {
        this.appConfig = appConfig;
        this.webClient = builder.baseUrl(appConfig.getBackendApiBase()).build();
    }

    @Override
    public String getName() {
        return "createBatchExamples";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "name", getName(),
                "description", "Creates examples in OFBiz.",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "count", Map.of("type", "number", "default", 5),
                                "prefix", Map.of("type", "string"),
                                "description", Map.of("type", "string"))));
    }

    @Override
    public Object execute(Map<String, Object> arguments, String downstreamToken) {
        int count = ((Number) arguments.getOrDefault("count", 5)).intValue();
        String prefix = (String) arguments.getOrDefault("prefix", "Example");
        String description = (String) arguments.getOrDefault("description", "Created via MCP");

        List<String> createdIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String name = prefix + " " + (i + 1);
            Map<String, Object> payload = Map.of(
                    "exampleName", name,
                    "exampleTypeId", "CONTRIVED",
                    "statusId", "EXST_IN_DESIGN",
                    "description", description);

            try {
                String responseBody = webClient.post()
                        .uri("/rest/example-rest/example")
                        .header("Authorization", "Bearer " + appConfig.getBackendAccessToken())
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode jsonNode = objectMapper.readTree(responseBody);
                if (jsonNode.has("data") && jsonNode.get("data").has("exampleId")) {
                    createdIds.add(jsonNode.get("data").get("exampleId").asText());
                } else {
                    errors.add("Failed to parse ID from response for index " + i);
                }

            } catch (Exception e) {
                errors.add("Error creating example " + i + ": " + e.getMessage());
            }
        }

        String resultText = "Created " + createdIds.size() + " examples. IDs: " + createdIds;
        if (!errors.isEmpty()) {
            resultText += "\nErrors: " + errors;
        }

        return Map.of("content", java.util.List.of(
                Map.of("type", "text", "text", resultText)));
    }
}
