package org.apache.ofbiz.mcp.tools;

import org.springframework.web.reactive.function.client.WebClient;
import org.apache.ofbiz.mcp.config.AppConfig;
import org.springframework.stereotype.Component;
import java.time.Instant;

import java.util.List;
import java.util.Map;

@Component
public class UpdateExamplesTool implements ToolHandler {

    private final AppConfig appConfig;
    private final WebClient webClient;

    public UpdateExamplesTool(AppConfig appConfig, WebClient.Builder builder) {
        this.appConfig = appConfig;
        this.webClient = builder.baseUrl(appConfig.getBackendApiBase()).build();
    }

    @Override
    public String getName() {
        return "updateExamples";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of(
                "name", getName(),
                "description", "Updates batch of example records in OFBiz.",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "ids", Map.of("type", "array", "items", Map.of("type", "string")),
                                "description", Map.of("type", "string"))));
    }

    @Override
    public Object execute(Map<String, Object> arguments, String downstreamToken) {
        List<String> ids = (List<String>) arguments.getOrDefault("ids", List.of());
        String descriptionTemplate = (String) arguments.getOrDefault("description", "Updated at " + Instant.now());

        List<String> results = reactor.core.publisher.Flux.fromIterable(ids)
                .flatMap(id -> {
                    String description = descriptionTemplate + " " + Instant.now();
                    Map<String, Object> payload = Map.of(
                            "exampleId", id,
                            "description", description);

                    return webClient.put()
                            .uri("/rest/example-rest/example")
                            .header("Authorization", "Bearer " + appConfig.getBackendAccessToken())
                            .bodyValue(payload)
                            .retrieve()
                            .toBodilessEntity()
                            .thenReturn(id)
                            .onErrorResume(e -> reactor.core.publisher.Mono
                                    .just("Error updating example " + id + ": " + e.getMessage()));
                })
                .collectList()
                .block();

        String resultText = "Updated " + results.size() + " examples. IDs: " + results;

        return Map.of("content", java.util.List.of(
                Map.of("type", "text", "text", resultText)));
    }
}
