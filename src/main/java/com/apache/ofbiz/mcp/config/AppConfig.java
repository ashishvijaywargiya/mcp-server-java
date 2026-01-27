package com.apache.ofbiz.mcp.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
    @JsonProperty("SERVER_PORT")
    private int serverPort;

    @JsonProperty("TLS_CERT_PATH")
    private String tlsCertPath;

    @JsonProperty("TLS_KEY_PATH")
    private String tlsKeyPath;

    @JsonProperty("TLS_KEY_PASSPHRASE")
    private String tlsKeyPassphrase;

    @JsonProperty("BACKEND_API_BASE")
    private String backendApiBase;

    @JsonProperty("BACKEND_USER_AGENT")
    private String backendUserAgent;

    @JsonProperty("BACKEND_ACCESS_TOKEN")
    private String backendAccessToken;

    @JsonProperty("AUTHZ_SERVER_BASE_URL")
    private String authzServerBaseUrl;

    @JsonProperty("MCP_SERVER_CLIENT_ID")
    private String mcpServerClientId;

    @JsonProperty("RATE_LIMIT_WINDOW_MS")
    private int rateLimitWindowMs = 60000;

    @JsonProperty("RATE_LIMIT_MAX_REQUESTS")
    private int rateLimitMaxRequests = 100;

    @JsonProperty("MCP_SERVER_CORS_ORIGINS")
    private String corsOrigins;
}
