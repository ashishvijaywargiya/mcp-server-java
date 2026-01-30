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
import java.util.Map;

/**
 * Dedicated runner for CreateExamplesTool to facilitate local testing
 * with self-signed certificates.
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
        System.out.println("Loaded configuration from " + configFile.getAbsolutePath());

        // 2. Configure SSL to trust all (Insecure - for local dev only)
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext));

        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        // 3. Instantiate the tool
        CreateExamplesTool tool = new CreateExamplesTool(config, webClientBuilder);

        // 4. Execute the tool
        int count = 20;
        String prefix = "RunnerEx";
        System.out.println("Executing CreateExamplesTool with count=" + count + ", prefix='" + prefix + "'");

        Map<String, Object> arguments = Map.of("count", count, "prefix", prefix);

        // Pass null for downstreamToken so it uses the one in AppConfig
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(arguments, null);

        // 5. Output results
        System.out.println("Execution finished.");
        System.out.println("Result content: " + result.get("content"));
    }
}
