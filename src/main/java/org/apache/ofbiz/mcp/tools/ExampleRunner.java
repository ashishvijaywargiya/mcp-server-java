package org.apache.ofbiz.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ofbiz.mcp.config.AppConfig;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dedicated runner for MCP Tools to facilitate local testing
 * with self-signed certificates.
 * 
 * Usage:
 * mvn exec:java -Dexec.mainClass="org.apache.ofbiz.mcp.tools.ExampleRunner"
 * -Dexec.args="create"
 * mvn exec:java -Dexec.mainClass="org.apache.ofbiz.mcp.tools.ExampleRunner"
 * -Dexec.args="update 10100 10101"
 * mvn exec:java -Dexec.mainClass="org.apache.ofbiz.mcp.tools.ExampleRunner"
 * -Dexec.args="delete 5"
 * mvn exec:java -Dexec.mainClass="org.apache.ofbiz.mcp.tools.ExampleRunner"
 * -Dexec.args="test-all"
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ExampleRunner...");

        // 1. Load configuration
        ObjectMapper mapper = new ObjectMapper();
        File configFile = new File("config/config.json");
        if (!configFile.exists()) {
            System.err.println("Error: config/config.json not found. Please run this from the project root.");
            System.exit(1);
        }
        AppConfig config = mapper.readValue(configFile, AppConfig.class);

        // 2. Configure SSL to trust all (Insecure - for local dev only)
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext));

        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        // 3. Instantiate tools
        CreateExamplesTool createTool = new CreateExamplesTool(config, webClientBuilder);
        UpdateExamplesTool updateTool = new UpdateExamplesTool(config, webClientBuilder);
        DeleteExamplesTool deleteTool = new DeleteExamplesTool(config, webClientBuilder);

        // 4. Determine action
        String action = args.length > 0 ? args[0] : "create";
        String[] actionArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (action.toLowerCase()) {
            case "create":
                runCreate(createTool, actionArgs);
                break;
            case "update":
                runUpdate(updateTool, actionArgs);
                break;
            case "delete":
                runDelete(deleteTool, actionArgs);
                break;
            case "test-all":
                runTestSequence(createTool, updateTool, deleteTool);
                break;
            case "test-scenario":
                // Args: createCount, deleteCount
                int cCount = actionArgs.length > 0 ? Integer.parseInt(actionArgs[0]) : 5;
                int dCount = actionArgs.length > 1 ? Integer.parseInt(actionArgs[1]) : 2;
                runCustomScenario(createTool, updateTool, deleteTool, cCount, dCount);
                break;
            case "update-last":
                // Args: count
                int uCount = actionArgs.length > 0 ? Integer.parseInt(actionArgs[0]) : 5;
                runUpdateLast(updateTool, config, webClientBuilder, uCount);
                break;
            default:
                System.out.println("Unknown action: " + action
                        + ". Use 'create', 'update', 'delete', 'test-all', or 'test-scenario'.");
        }
    }

    private static void runCreate(CreateExamplesTool tool, String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        String prefix = args.length > 1 ? args[1] : "RunnerEx";

        System.out.println("--- Running CreateExamplesTool ---");
        System.out.println("Count: " + count + ", Prefix: " + prefix);

        Map<String, Object> arguments = Map.of("count", count, "prefix", prefix);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(arguments, null);

        System.out.println("Result: " + result.get("content"));
    }

    private static void runUpdate(UpdateExamplesTool tool, String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: update <id1> <id2> ...");
            return;
        }
        List<String> ids = Arrays.asList(args);

        System.out.println("--- Running UpdateExamplesTool ---");
        System.out.println("Updating IDs: " + ids);

        Map<String, Object> arguments = Map.of(
                "ids", ids,
                "description", "Updated via ExampleRunner at " + java.time.Instant.now());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(arguments, null);

        System.out.println("Result: " + result.get("content"));
    }

    private static void runDelete(DeleteExamplesTool tool, String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 5;

        System.out.println("--- Running DeleteExamplesTool ---");
        System.out.println("Deleting last " + count + " examples");

        Map<String, Object> arguments = Map.of("count", count);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(arguments, null);

        System.out.println("Result: " + result.get("content"));
    }

    private static void runTestSequence(CreateExamplesTool create, UpdateExamplesTool update,
            DeleteExamplesTool delete) {
        runCustomScenario(create, update, delete, 3, 3);
    }

    private static void runCustomScenario(CreateExamplesTool create, UpdateExamplesTool update,
            DeleteExamplesTool delete, int createCount, int deleteCount) {
        System.out.println("=== Starting Custom Scenario ===");
        System.out.println("Plan: Create " + createCount + " -> Update All -> Delete " + deleteCount);

        // 1. Create
        System.out.println("\n[1/3] Creating " + createCount + " examples...");
        Map<String, Object> createArgs = Map.of("count", createCount, "prefix", "CustomEx");
        Map<String, Object> createResult = (Map<String, Object>) create.execute(createArgs, null);
        System.out.println("Create Result: " + createResult.get("content"));

        // Parse IDs
        String resultText = createResult.get("content").toString();
        Pattern pattern = Pattern.compile("(\\d{5})"); // Match 5-digit IDs
        Matcher matcher = pattern.matcher(resultText);
        List<String> createdIds = new java.util.ArrayList<>();
        while (matcher.find()) {
            createdIds.add(matcher.group(1));
        }

        if (createdIds.isEmpty()) {
            System.out.println("No IDs created, aborting sequence.");
            return;
        }
        System.out.println("Captured IDs: " + createdIds);

        // 2. Update
        System.out.println("\n[2/3] Updating created examples: " + createdIds);
        Map<String, Object> updateArgs = Map.of(
                "ids", createdIds,
                "description", "Scenario Update at " + java.time.Instant.now());
        Map<String, Object> updateResult = (Map<String, Object>) update.execute(updateArgs, null);
        System.out.println("Update Result: " + updateResult.get("content"));

        // 3. Delete
        System.out.println("\n[3/3] Deleting " + deleteCount + " examples...");
        Map<String, Object> deleteArgs = Map.of("count", deleteCount);
        Map<String, Object> deleteResult = (Map<String, Object>) delete.execute(deleteArgs, null);

        // Parse Deleted IDs (for verification)
        String deleteText = deleteResult.get("content").toString();
        matcher = pattern.matcher(deleteText);
        List<String> deletedIds = new java.util.ArrayList<>();
        while (matcher.find()) {
            deletedIds.add(matcher.group(1));
        }
        System.out.println("Delete Result: " + deleteResult.get("content"));
        System.out.println("Deleted IDs: " + deletedIds);

        System.out.println("\n=== Scenario Completed ===");
    }

    private static void runUpdateLast(UpdateExamplesTool tool, AppConfig config, WebClient.Builder webClientBuilder,
            int count) {
        System.out.println("--- Running UpdateExamplesTool (Last " + count + ") ---");

        // 1. Fetch IDs (Using logic similar to DeleteExamplesTool)
        System.out.println("Fetching last " + count + " examples...");
        WebClient client = webClientBuilder.baseUrl(config.getBackendApiBase()).build();
        ObjectMapper mapper = new ObjectMapper();

        List<String> ids;
        try {
            String resp = client.get().uri("/rest/example-rest/example")
                    .header("Authorization", "Bearer " + config.getBackendAccessToken())
                    .retrieve()
                    .bodyToMono(String.class).block();

            ids = java.util.stream.StreamSupport
                    .stream(mapper.readTree(resp).path("data").path("exampleList").spliterator(), false)
                    .map(n -> n.path("exampleId").asText())
                    .sorted((s1, s2) -> Long.compare(Long.parseLong(s2), Long.parseLong(s1)))
                    .limit(count)
                    .collect(java.util.stream.Collectors.toList());

            if (ids.isEmpty()) {
                System.out.println("No examples found to update.");
                return;
            }
            System.out.println("Found IDs: " + ids);

        } catch (Exception e) {
            System.err.println("Error fetching IDs: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 2. Execute Update
        System.out.println("Updating " + ids.size() + " examples...");
        Map<String, Object> arguments = Map.of(
                "ids", ids,
                "description", "Bulk Update via Runner "); // Timestamp is added by the tool

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(arguments, null);
        System.out.println("Result: " + result.get("content"));
    }
}
