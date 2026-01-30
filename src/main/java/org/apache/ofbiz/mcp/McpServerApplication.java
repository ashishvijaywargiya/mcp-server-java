package org.apache.ofbiz.mcp;

import org.apache.ofbiz.mcp.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.IOException;

@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar mcp-server.jar <path-to-config-folder> [path-to-tools-folder]");
            System.exit(1);
        }
        String configPath = args[0];
        // Set configuration path property for injection
        System.setProperty("mcp.config.path", configPath);
        if (args.length > 1) {
            System.setProperty("mcp.tools.path", args[1]);
        }

        // Pre-load config to set server port
        try {
            ObjectMapper mapper = new ObjectMapper();
            AppConfig config = mapper.readValue(new File(configPath + "/config.json"), AppConfig.class);
            System.setProperty("server.port", String.valueOf(config.getServerPort()));
        } catch (IOException e) {
            System.err.println("Failed to load config.json: " + e.getMessage());
            System.exit(1);
        }

        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public AppConfig appConfig() throws IOException {
        String configPath = System.getProperty("mcp.config.path");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(configPath + "/config.json"), AppConfig.class);
    }
}
