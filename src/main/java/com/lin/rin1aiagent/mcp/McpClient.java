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
}
