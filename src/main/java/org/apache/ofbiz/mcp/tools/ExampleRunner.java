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
            default:
                System.out.println("Unknown action: " + action + ". Use 'create', 'update', 'delete', or 'test-all'.");
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
        System.out.println("=== Starting Full Test Sequence ===");

        // 1. Create
        System.out.println("\n[1/3] Creating 3 examples...");
        Map<String, Object> createArgs = Map.of("count", 3, "prefix", "SeqTest");
        Map<String, Object> createResult = (Map<String, Object>) create.execute(createArgs, null);
        System.out.println("Create Result: " + createResult.get("content"));

        // Parse IDs from result (A bit hacky but works for the runner)
        // Expected format: Result: [10100, 10101, ...]
        String resultText = createResult.get("content").toString();
        Pattern pattern = Pattern.compile("(\\d{5})");
        Matcher matcher = pattern.matcher(resultText);
        List<String> createdIds = new java.util.ArrayList<>();
        while (matcher.find()) {
            createdIds.add(matcher.group(1));
        }

        if (createdIds.isEmpty()) {
            System.out.println("No IDs created, aborting sequence.");
            return;
        }

        // 2. Update
        System.out.println("\n[2/3] Updating created examples: " + createdIds);
        Map<String, Object> updateArgs = Map.of(
                "ids", createdIds,
                "description", "Sequence Update Test");
        Map<String, Object> updateResult = (Map<String, Object>) update.execute(updateArgs, null);
        System.out.println("Update Result: " + updateResult.get("content"));

        // 3. Delete
        System.out.println("\n[3/3] Deleting the 3 examples...");
        Map<String, Object> deleteArgs = Map.of("count", 3);
        Map<String, Object> deleteResult = (Map<String, Object>) delete.execute(deleteArgs, null);
        System.out.println("Delete Result: " + deleteResult.get("content"));

        System.out.println("\n=== Test Sequence Completed ===");
    }
}
