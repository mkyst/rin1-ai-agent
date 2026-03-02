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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class McpToolCallbackProviderConfig {
    private final Map<String, McpClient> mcpClients = new HashMap<>();

    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        try {
            // Read all MCP server configurations
            McpConfigReader configReader = new McpConfigReader();
            Map<String, McpServerConfig> serverConfigs = configReader.readAllConfigs();

            if (serverConfigs.isEmpty()) {
                log.warn("No MCP servers configured");
                return () -> new org.springframework.ai.tool.ToolCallback[0];
            }

            // Initialize all MCP clients
            int totalTools = 0;
            for (Map.Entry<String, McpServerConfig> entry : serverConfigs.entrySet()) {
                String serverName = entry.getKey();
                McpServerConfig serverConfig = entry.getValue();

                try {
                    log.info("Initializing MCP server: {}", serverName);
                    McpClient client = new McpClient(serverConfig);
                    client.initialize();

                    // Discover tools
                    List<McpToolDefinition> tools = client.listTools();
                    log.info("Loaded {} tools from MCP server: {}", tools.size(), serverName);

                    for (McpToolDefinition tool : tools) {
                        log.info("  - {} : {}", tool.getName(), tool.getDescription());
                    }

                    mcpClients.put(serverName, client);
                    totalTools += tools.size();

                } catch (Exception e) {
                    log.error("Failed to initialize MCP server: {}", serverName, e);
                    // Continue with other servers
                }
            }

            log.info("Successfully initialized {} MCP servers with {} total tools",
                    mcpClients.size(), totalTools);

            // Return empty provider for now (tools are registered but not yet converted to ToolCallbacks)
            return () -> new org.springframework.ai.tool.ToolCallback[0];

        } catch (Exception e) {
            log.error("Failed to initialize MCP tools, returning empty provider", e);
            return () -> new org.springframework.ai.tool.ToolCallback[0];
        }
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
