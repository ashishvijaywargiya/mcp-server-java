package com.apache.ofbiz.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcRequest {
    private String jsonrpc = "2.0";
    private String method;
    private Map<String, Object> params;
    private Object id;
}
