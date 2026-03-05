package com.lin.rin1aiagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lin.rin1aiagent.mcp.McpClient;
import com.lin.rin1aiagent.mcp.McpConfigReader;
import com.lin.rin1aiagent.mcp.model.McpServerConfig;
import com.lin.rin1aiagent.mcp.model.McpToolDefinition;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Configuration
@Slf4j
public class McpToolCallbackProviderConfig {

    private static final String DEFAULT_INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": true
            }
            """;

    private final Map<String, McpClient> mcpClients = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        try {
            McpConfigReader configReader = new McpConfigReader();
            Map<String, McpServerConfig> serverConfigs = configReader.readAllConfigs();

            if (serverConfigs.isEmpty()) {
                log.warn("No MCP servers configured");
                return ToolCallbackProvider.from();
            }

            int totalDiscoveredTools = 0;
            List<ToolCallback> callbacks = new ArrayList<>();
            Set<String> usedToolNames = new HashSet<>();

            for (Map.Entry<String, McpServerConfig> entry : serverConfigs.entrySet()) {
                String serverName = entry.getKey();
                McpServerConfig serverConfig = entry.getValue();

                try {
                    log.info("Initializing MCP server: {}", serverName);
                    McpClient client = new McpClient(serverConfig);
                    client.initialize();

                    List<McpToolDefinition> tools = client.listTools();
                    totalDiscoveredTools += tools.size();
                    mcpClients.put(serverName, client);

                    for (McpToolDefinition tool : tools) {
                        ToolCallback callback = buildToolCallback(serverName, client, tool, usedToolNames);
                        callbacks.add(callback);
                        log.info("Registered MCP tool callback: {} -> {}.{}",
                                callback.getToolDefinition().name(),
                                serverName,
                                tool.getName());
                    }

                } catch (Exception e) {
                    log.error("Failed to initialize MCP server: {}", serverName, e);
                }
            }

            log.info("Initialized {} MCP servers, discovered {} tools, registered {} callbacks",
                    mcpClients.size(), totalDiscoveredTools, callbacks.size());

            return ToolCallbackProvider.from(callbacks);
        } catch (Exception e) {
            log.error("Failed to initialize MCP tools, returning empty provider", e);
            return ToolCallbackProvider.from();
        }
    }

    private ToolCallback buildToolCallback(String serverName,
                                           McpClient client,
                                           McpToolDefinition tool,
                                           Set<String> usedToolNames) {
        String rawToolName = normalizeName(tool.getName(), "mcp_tool");
        String exposedToolName = ensureUniqueToolName(serverName, rawToolName, usedToolNames);
        String toolDescription = buildToolDescription(serverName, tool, exposedToolName, rawToolName);
        String inputSchemaJson = toInputSchemaJson(tool.getInputSchema());

        return FunctionToolCallback
                .builder(exposedToolName, (Map<String, Object> input, ToolContext ignoredContext) ->
                        invokeMcpTool(serverName, client, rawToolName, input))
                .description(toolDescription)
                .inputSchema(inputSchemaJson)
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .build();
    }

    private String invokeMcpTool(String serverName,
                                 McpClient client,
                                 String toolName,
                                 Map<String, Object> input) {
        try {
            Map<String, Object> args = input != null ? input : Map.of();
            Map<String, Object> result = client.callTool(toolName, args);
            if (result == null || result.isEmpty()) {
                return "MCP tool returned empty result.";
            }
            return objectMapper.writeValueAsString(result);
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Failed to call MCP tool %s.%s: %s", serverName, toolName, e.getMessage()), e);
        }
    }

    private String toInputSchemaJson(Map<String, Object> inputSchema) {
        if (inputSchema == null || inputSchema.isEmpty()) {
            return DEFAULT_INPUT_SCHEMA;
        }
        try {
            return objectMapper.writeValueAsString(inputSchema);
        } catch (Exception e) {
            log.warn("Failed to serialize MCP input schema, using default schema", e);
            return DEFAULT_INPUT_SCHEMA;
        }
    }

    private String buildToolDescription(String serverName,
                                        McpToolDefinition tool,
                                        String exposedToolName,
                                        String rawToolName) {
        String baseDescription = tool.getDescription();
        String sourceInfo = "[MCP server: " + serverName + ", original tool: " + rawToolName + "]";
        if (baseDescription == null || baseDescription.isBlank()) {
            return sourceInfo;
        }
        if (Objects.equals(exposedToolName, rawToolName)) {
            return baseDescription + " " + sourceInfo;
        }
        return baseDescription + " " + sourceInfo + " (exposed name: " + exposedToolName + ")";
    }

    private String normalizeName(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return name.trim();
    }

    private String ensureUniqueToolName(String serverName, String rawToolName, Set<String> usedToolNames) {
        String candidate = rawToolName;
        if (usedToolNames.add(candidate)) {
            return candidate;
        }

        candidate = serverName + "_" + rawToolName;
        if (usedToolNames.add(candidate)) {
            return candidate;
        }

        int index = 2;
        while (!usedToolNames.add(candidate + "_" + index)) {
            index++;
        }
        return candidate + "_" + index;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down {} MCP clients", mcpClients.size());
        for (Map.Entry<String, McpClient> entry : mcpClients.entrySet()) {
            try {
                log.info("Shutting down MCP server: {}", entry.getKey());
                entry.getValue().shutdown();
            } catch (Exception e) {
                log.error("Error shutting down MCP server: {}", entry.getKey(), e);
            }
        }
        mcpClients.clear();
    }
}
