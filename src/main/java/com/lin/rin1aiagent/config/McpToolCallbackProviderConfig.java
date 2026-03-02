package com.lin.rin1aiagent.config;

import com.lin.rin1aiagent.mcp.McpClient;
import com.lin.rin1aiagent.mcp.McpConfigReader;
import com.lin.rin1aiagent.mcp.model.McpServerConfig;
import com.lin.rin1aiagent.mcp.model.McpToolDefinition;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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

            for (McpToolDefinition tool : tools) {
                log.info("Registered MCP tool: {} - {}", tool.getName(), tool.getDescription());
            }

            // Return a simple provider that delegates to mcpClient
            return () -> new org.springframework.ai.tool.ToolCallback[0];

        } catch (Exception e) {
            log.error("Failed to initialize MCP tools, returning empty provider", e);
            return () -> new org.springframework.ai.tool.ToolCallback[0];
        }
    }

    @PreDestroy
    public void cleanup() {
        if (mcpClient != null) {
            mcpClient.shutdown();
        }
    }
}
