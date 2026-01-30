package org.apache.ofbiz.mcp.tools;

import org.apache.ofbiz.mcp.config.AppConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

@Component
public class CreateExamplesTool implements ToolHandler {
    private final AppConfig appConfig;
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public CreateExamplesTool(AppConfig cfg, WebClient.Builder b) {
        this.appConfig = cfg;
        this.webClient = b.baseUrl(cfg.getBackendApiBase()).build();
    }

    @Override
    public String getName() {
        return "createExamples";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of("name", getName(), "description", "Creates examples", "inputSchema", Map.of("type", "object",
                "properties", Map.of("count", Map.of("type", "number"), "prefix", Map.of("type", "string"))));
    }

    @Override
    public Object execute(Map<String, Object> serverArgs, String downstreamToken) {
        int count = ((Number) serverArgs.getOrDefault("count", 5)).intValue();
        String prefix = (String) serverArgs.getOrDefault("prefix", "Ex");

        List<String> results = reactor.core.publisher.Flux.range(0, count)
                .flatMap(i -> webClient.post().uri("/rest/example-rest/example")
                        .header("Authorization", "Bearer " + appConfig.getBackendAccessToken())
                        .bodyValue(Map.of("exampleName", prefix + " " + (i + 1), "exampleTypeId", "CONTRIVED",
                                "statusId", "EXST_IN_DESIGN"))
                        .retrieve().bodyToMono(String.class)
                        .map(resp -> {
                            try {
                                return mapper.readTree(resp).path("data").path("exampleId").asText();
                            } catch (Exception e) {
                                return "Error parsing: " + e.getMessage();
                            }
                        })
                        .onErrorResume(e -> reactor.core.publisher.Mono.just("Error: " + e.getMessage())))
                .collectList().block();

        return Map.of("content", List.of(Map.of("type", "text", "text", "Result: " + results)));
    }
}
