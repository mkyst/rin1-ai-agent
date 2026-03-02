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

    @PreDestroy
    public void cleanup() {
        if (mcpClient != null) {
            mcpClient.shutdown();
        }
    }
}
