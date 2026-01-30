package org.apache.ofbiz.mcp.tools;

import org.apache.ofbiz.mcp.config.AppConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class DeleteExamplesTool implements ToolHandler {
    private final AppConfig cfg;
    private final WebClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeleteExamplesTool(AppConfig c, WebClient.Builder b) {
        this.cfg = c;
        this.client = b.baseUrl(c.getBackendApiBase()).build();
    }

    @Override
    public String getName() {
        return "deleteExamples";
    }

    @Override
    public Map<String, Object> getDefinition() {
        return Map.of("name", getName(), "description", "Deletes last N examples (highest ID)", "inputSchema",
                Map.of("type", "object", "required", List.of("count"), "properties",
                        Map.of("count", Map.of("type", "number", "description", "Number of examples to delete"))));
    }

    @Override
    public Object execute(Map<String, Object> args, String t) {
        if (!args.containsKey("count"))
            throw new IllegalArgumentException("Required parameter 'count' missing");
        int count = ((Number) args.get("count")).intValue();
        try {
            String resp = client.get().uri("/rest/example-rest/example")
                    .header("Authorization", "Bearer " + cfg.getBackendAccessToken()).retrieve()
                    .bodyToMono(String.class).block();
            List<String> ids = StreamSupport
                    .stream(mapper.readTree(resp).path("data").path("exampleList").spliterator(), false)
                    .map(n -> n.path("exampleId").asText())
                    .sorted((s1, s2) -> Long.compare(Long.parseLong(s2), Long.parseLong(s1))) // Explicit comparator for
                                                                                              // reverse sort
                    .limit(count).collect(Collectors.toList());

            reactor.core.publisher.Flux.fromIterable(ids)
                    .flatMap(id -> client.delete()
                            .uri(u -> u.path("/rest/example-rest/example").queryParam("exampleId", id).build())
                            .header("Authorization", "Bearer " + cfg.getBackendAccessToken()).retrieve()
                            .toBodilessEntity())
                    .collectList()
                    .block();

            return Map.of("content",
                    List.of(Map.of("type", "text", "text", "Deleted " + ids.size() + " examples: " + ids)));
        } catch (Exception e) {
            return Map.of("content", List.of(Map.of("type", "text", "text", "Error: " + e.getMessage())));
        }
    }
}
