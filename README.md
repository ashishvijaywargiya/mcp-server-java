# MCP Server for Apache OFBiz (Java Version)

This is a Java Spring Boot implementation of the MCP server, mirroring the functionality of the Node.js version.

## Prerequisites

- Java 17+
- Maven 3.8+

## Build

```bash
mvn clean install
```

## Run

To run the server, point it to your existing configuration directory:

```bash
java -jar target/mcp-server-0.0.1-SNAPSHOT.jar ../config
```

## Structure

- `src/main/java/com/apache/ofbiz/mcp/McpServerApplication.java`: Entry point.
- `src/main/java/com/apache/ofbiz/mcp/controller/McpController.java`: Handles MCP protocol (JSON-RPC over HTTP).
- `src/main/java/com/apache/ofbiz/mcp/tools/`: Tool logic.

## Logic to Implement

The tool classes currently contain placeholders. You need to implement the actual WebClient logic to call OFBiz APIs, similar to how it is done in the Node.js version using `fetch`.
