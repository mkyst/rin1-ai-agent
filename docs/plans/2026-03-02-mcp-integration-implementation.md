# MCP Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Integrate MCP (Model Context Protocol) client to connect to Amap Maps server and expose tools to Spring AI ChatClient.

**Architecture:** Build McpClient to manage MCP server process via stdio, implement JSON-RPC 2.0 protocol for tool discovery and execution, create Spring configuration to register MCP tools as ToolCallbackProvider bean.

**Tech Stack:** Java 21, Spring Boot 3.5, Jackson JSON, ProcessBuilder, Spring AI

---

## Task 1: Create MCP Data Models

**Files:**
- Create: `src/main/java/com/lin/rin1aiagent/mcp/model/McpRequest.java`
- Create: `src/main/java/com/lin/rin1aiagent/mcp/model/McpResponse.java`
- Create: `src/main/java/com/lin/rin1aiagent/mcp/model/McpToolDefinition.java`
- Create: `src/main/java/com/lin/rin1aiagent/mcp/model/McpServerConfig.java`

**Step 1: Create McpRequest class**

```java
package com.lin.rin1aiagent.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Map<String, Object> params;
}
```

**Step 2: Create McpResponse class**

```java
package com.lin.rin1aiagent.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class McpResponse {
    @JsonProperty("jsonrpc")
    private String jsonrpc;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("result")
    private Map<String, Object> result;

    @JsonProperty("error")
    private McpError error;

    @Data
    @NoArgsConstructor
    public static class McpError {
        @JsonProperty("code")
        private Integer code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("data")
        private Object data;
    }
}
```

**Step 3: Create McpToolDefinition class**

```java
package com.lin.rin1aiagent.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class McpToolDefinition {
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inputSchema")
    private Map<String, Object> inputSchema;
}
```

**Step 4: Create McpServerConfig class**

```java
package com.lin.rin1aiagent.mcp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class McpServerConfig {
    private String command;
    private List<String> args;
    private Map<String, String> env;
}
```

**Step 5: Commit data models**

```bash
git add src/main/java/com/lin/rin1aiagent/mcp/model/
git commit -m "feat: add MCP data models for JSON-RPC communication"
```

---

## Task 2: Create McpClient Core

**Files:**
- Create: `src/main/java/com/lin/rin1aiagent/mcp/McpClient.java`

**Step 1: Create McpClient skeleton**

```java
package com.lin.rin1aiagent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lin.rin1aiagent.mcp.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class McpClient {
    private final McpServerConfig config;
    private final ObjectMapper objectMapper;
    private Process process;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);

    public McpClient(McpServerConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public void initialize() throws IOException {
        // Will implement in next step
    }

    public List<McpToolDefinition> listTools() throws IOException {
        // Will implement in next step
        return Collections.emptyList();
    }

    public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) throws IOException {
        // Will implement in next step
        return Collections.emptyMap();
    }

    public void shutdown() {
        // Will implement in next step
    }
}
```

**Step 2: Implement initialize method**

```java
public void initialize() throws IOException {
    log.info("Starting MCP server: {} {}", config.getCommand(), config.getArgs());

    ProcessBuilder processBuilder = new ProcessBuilder();
    List<String> command = new ArrayList<>();
    command.add(config.getCommand());
    command.addAll(config.getArgs());
    processBuilder.command(command);

    // Set environment variables
    if (config.getEnv() != null) {
        processBuilder.environment().putAll(config.getEnv());
    }

    processBuilder.redirectErrorStream(false);

    try {
        process = processBuilder.start();
        processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // Send initialize request
        McpRequest initRequest = new McpRequest();
        initRequest.setId(requestIdCounter.getAndIncrement());
        initRequest.setMethod("initialize");
        initRequest.setParams(Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(),
            "clientInfo", Map.of(
                "name", "rin1-ai-agent",
                "version", "1.0.0"
            )
        ));

        McpResponse initResponse = sendRequest(initRequest);
        log.info("MCP server initialized: {}", initResponse);

        // Send initialized notification
        McpRequest initializedNotification = new McpRequest();
        initializedNotification.setMethod("notifications/initialized");
        initializedNotification.setParams(Map.of());
        String notificationJson = objectMapper.writeValueAsString(initializedNotification);
        processInput.write(notificationJson);
        processInput.newLine();
        processInput.flush();

    } catch (IOException e) {
        log.error("Failed to start MCP server", e);
        throw e;
    }
}
```

**Step 3: Implement sendRequest helper method**

```java
private McpResponse sendRequest(McpRequest request) throws IOException {
    String requestJson = objectMapper.writeValueAsString(request);
    log.debug("Sending MCP request: {}", requestJson);

    processInput.write(requestJson);
    processInput.newLine();
    processInput.flush();

    String responseLine = processOutput.readLine();
    if (responseLine == null) {
        throw new IOException("MCP server closed connection");
    }

    log.debug("Received MCP response: {}", responseLine);
    McpResponse response = objectMapper.readValue(responseLine, McpResponse.class);

    if (response.getError() != null) {
        throw new IOException("MCP error: " + response.getError().getMessage());
    }

    return response;
}
```

**Step 4: Implement listTools method**

```java
public List<McpToolDefinition> listTools() throws IOException {
    McpRequest request = new McpRequest();
    request.setId(requestIdCounter.getAndIncrement());
    request.setMethod("tools/list");
    request.setParams(Map.of());

    McpResponse response = sendRequest(request);

    Map<String, Object> result = response.getResult();
    if (result == null || !result.containsKey("tools")) {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> toolsData = (List<Map<String, Object>>) result.get("tools");

    List<McpToolDefinition> tools = new ArrayList<>();
    for (Map<String, Object> toolData : toolsData) {
        McpToolDefinition tool = objectMapper.convertValue(toolData, McpToolDefinition.class);
        tools.add(tool);
    }

    log.info("Discovered {} MCP tools", tools.size());
    return tools;
}
```

**Step 5: Implement callTool method**

```java
public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) throws IOException {
    McpRequest request = new McpRequest();
    request.setId(requestIdCounter.getAndIncrement());
    request.setMethod("tools/call");
    request.setParams(Map.of(
        "name", toolName,
        "arguments", arguments != null ? arguments : Map.of()
    ));

    McpResponse response = sendRequest(request);
    return response.getResult();
}
```

**Step 6: Implement shutdown method**

```java
public void shutdown() {
    if (process != null && process.isAlive()) {
        log.info("Shutting down MCP server");
        try {
            processInput.close();
            processOutput.close();
            process.destroy();
            process.waitFor(5, TimeUnit.SECONDS);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            log.error("Error shutting down MCP server", e);
        }
    }
}
```

**Step 7: Commit McpClient**

```bash
git add src/main/java/com/lin/rin1aiagent/mcp/McpClient.java
git commit -m "feat: implement McpClient for MCP server communication"
```

---

## Task 3: Create Configuration Reader

**Files:**
- Create: `src/main/java/com/lin/rin1aiagent/mcp/McpConfigReader.java`

**Step 1: Create McpConfigReader class**

```java
package com.lin.rin1aiagent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lin.rin1aiagent.mcp.model.McpServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Slf4j
public class McpConfigReader {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpServerConfig readAmapConfig() throws IOException {
        ClassPathResource resource = new ClassPathResource("mcp-servers.json");

        try (InputStream inputStream = resource.getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(inputStream, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> mcpServers = (Map<String, Object>) config.get("mcpServers");

            if (mcpServers == null || !mcpServers.containsKey("amap-maps")) {
                throw new IOException("amap-maps configuration not found in mcp-servers.json");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> amapConfig = (Map<String, Object>) mcpServers.get("amap-maps");

            return objectMapper.convertValue(amapConfig, McpServerConfig.class);
        }
    }
}
```

**Step 2: Commit configuration reader**

```bash
git add src/main/java/com/lin/rin1aiagent/mcp/McpConfigReader.java
git commit -m "feat: add configuration reader for mcp-servers.json"
```

---

## Task 4: Create Spring Configuration

**Files:**
- Create: `src/main/java/com/lin/rin1aiagent/config/McpToolCallbackProviderConfig.java`

**Step 1: Create configuration class skeleton**

```java
package com.lin.rin1aiagent.config;

import com.lin.rin1aiagent.mcp.McpClient;
import com.lin.rin1aiagent.mcp.McpConfigReader;
import com.lin.rin1aiagent.mcp.model.McpServerConfig;
import com.lin.rin1aiagent.mcp.model.McpToolDefinition;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class McpToolCallbackProviderConfig {
    private McpClient mcpClient;

    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        // Will implement in next steps
        return null;
    }

    @PreDestroy
    public void cleanup() {
        if (mcpClient != null) {
            mcpClient.shutdown();
        }
    }
}
```

**Step 2: Implement toolCallbackProvider bean**

```java
@Bean
public ToolCallbackProvider toolCallbackProvider() {
    try {
        // Read configuration
        McpConfigReader configReader = new McpConfigReader();
        McpServerConfig serverConfig = configReader.readAmapConfig();

        // Initialize MCP client
        mcpClient = new McpClient(serverConfig);
        mcpClient.initialize();

        // Discover tools
        List<McpToolDefinition> tools = mcpClient.listTools();
        log.info("Loaded {} MCP tools from amap-maps server", tools.size());

        // Convert to ToolCallbacks
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        for (McpToolDefinition tool : tools) {
            ToolCallback callback = createToolCallback(tool);
            toolCallbacks.add(callback);
            log.info("Registered MCP tool: {} - {}", tool.getName(), tool.getDescription());
        }

        return ToolCallbackProvider.of(toolCallbacks);

    } catch (Exception e) {
        log.error("Failed to initialize MCP tools, returning empty provider", e);
        return ToolCallbackProvider.of(List.of());
    }
}
```

**Step 3: Implement createToolCallback helper method**

```java
private ToolCallback createToolCallback(McpToolDefinition tool) {
    return ToolCallback.builder()
        .name(tool.getName())
        .description(tool.getDescription())
        .inputTypeSchema(tool.getInputSchema() != null ? tool.getInputSchema().toString() : "{}")
        .function((Map<String, Object> arguments) -> {
            try {
                log.debug("Calling MCP tool: {} with arguments: {}", tool.getName(), arguments);
                Map<String, Object> result = mcpClient.callTool(tool.getName(), arguments);
                log.debug("MCP tool {} returned: {}", tool.getName(), result);
                return result;
            } catch (Exception e) {
                log.error("Error calling MCP tool: {}", tool.getName(), e);
                return Map.of("error", e.getMessage());
            }
        })
        .build();
}
```

**Step 4: Commit configuration**

```bash
git add src/main/java/com/lin/rin1aiagent/config/McpToolCallbackProviderConfig.java
git commit -m "feat: add Spring configuration for MCP ToolCallbackProvider"
```

---

## Task 5: Run Integration Test

**Files:**
- Test: `src/test/java/com/lin/rin1aiagent/app/LoveAppTest.java`

**Step 1: Run the doChatWithMcp test**

Run: `mvn test -Dtest=LoveAppTest#doChatWithMcp`

Expected: Test should pass, or show MCP-related errors (not bean creation errors)

**Step 2: Check application logs**

Look for:
- "Starting MCP server: npx.cmd [-y, @amap/amap-maps-mcp-server]"
- "MCP server initialized"
- "Discovered X MCP tools"
- "Registered MCP tool: ..."

**Step 3: If test fails, debug**

Common issues:
- Node.js/npx not in PATH: Install Node.js or add to PATH
- MCP server startup timeout: Check network, increase timeout
- JSON parsing errors: Check MCP server output format

**Step 4: Commit if test passes**

```bash
git add -A
git commit -m "test: verify MCP integration with LoveAppTest"
```

---

## Task 6: Add Unit Tests for McpClient

**Files:**
- Create: `src/test/java/com/lin/rin1aiagent/mcp/McpClientTest.java`

**Step 1: Write test for JSON-RPC serialization**

```java
package com.lin.rin1aiagent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lin.rin1aiagent.mcp.model.McpRequest;
import com.lin.rin1aiagent.mcp.model.McpResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testRequestSerialization() throws Exception {
        McpRequest request = new McpRequest();
        request.setId(1);
        request.setMethod("tools/list");
        request.setParams(Map.of());

        String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"jsonrpc\":\"2.0\"");
        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"method\":\"tools/list\"");
    }

    @Test
    void testResponseDeserialization() throws Exception {
        String json = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"tools\":[]}}";

        McpResponse response = objectMapper.readValue(json, McpResponse.class);

        assertThat(response.getJsonrpc()).isEqualTo("2.0");
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getResult()).containsKey("tools");
    }
}
```

**Step 2: Run unit tests**

Run: `mvn test -Dtest=McpClientTest`

Expected: All tests pass

**Step 3: Commit unit tests**

```bash
git add src/test/java/com/lin/rin1aiagent/mcp/McpClientTest.java
git commit -m "test: add unit tests for McpClient JSON-RPC serialization"
```

---

## Task 7: Documentation and Cleanup

**Files:**
- Modify: `src/main/resources/mcp-servers.json`
- Create: `docs/mcp-integration.md`

**Step 1: Add comment to mcp-servers.json**

Add at the top:
```json
{
  "_comment": "MCP server configuration. Do not commit real API keys. Use environment variables in production.",
  "mcpServers": {
    ...
  }
}
```

**Step 2: Create integration documentation**

```markdown
# MCP Integration

## Overview

This application integrates with MCP (Model Context Protocol) servers to provide external tools to the AI chat client.

## Supported Servers

- **Amap Maps**: Location-based services (search nearby places, geocoding, etc.)

## Configuration

Edit `src/main/resources/mcp-servers.json`:

```json
{
  "mcpServers": {
    "amap-maps": {
      "command": "npx.cmd",
      "args": ["-y", "@amap/amap-maps-mcp-server"],
      "env": {
        "AMAP_MAPS_API_KEY": "your-api-key-here"
      }
    }
  }
}
```

## Requirements

- Node.js 18+ with npx
- Internet connection for MCP server installation

## Testing

Run integration test:
```bash
mvn test -Dtest=LoveAppTest#doChatWithMcp
```

## Troubleshooting

**Error: "Failed to start MCP server"**
- Check Node.js is installed: `node --version`
- Check npx is available: `npx --version`
- Check network connection

**Error: "MCP server closed connection"**
- Check API key is valid
- Check MCP server logs in application output
```

**Step 3: Commit documentation**

```bash
git add src/main/resources/mcp-servers.json docs/mcp-integration.md
git commit -m "docs: add MCP integration documentation"
```

---

## Success Criteria

- [ ] `LoveAppTest.doChatWithMcp()` test passes
- [ ] Application starts without errors
- [ ] MCP tools are registered and logged at startup
- [ ] AI can call Amap Maps tools successfully
- [ ] All unit tests pass

## Notes

- MCP server process runs as child process of the application
- Process is automatically cleaned up on application shutdown
- Graceful degradation: if MCP fails, application continues with empty tool list
- All MCP communication is logged at DEBUG level for troubleshooting
