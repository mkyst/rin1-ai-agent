package com.lin.rin1aiagent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lin.rin1aiagent.mcp.model.McpServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class McpConfigReader {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Read all MCP server configurations from mcp-servers.json
     */
    public Map<String, McpServerConfig> readAllConfigs() throws IOException {
        ClassPathResource resource = new ClassPathResource("mcp-servers.json");

        try (InputStream inputStream = resource.getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(inputStream, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> mcpServers = (Map<String, Object>) config.get("mcpServers");

            if (mcpServers == null || mcpServers.isEmpty()) {
                log.warn("No MCP servers found in mcp-servers.json");
                return new HashMap<>();
            }

            Map<String, McpServerConfig> serverConfigs = new HashMap<>();
            for (Map.Entry<String, Object> entry : mcpServers.entrySet()) {
                String serverName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> serverConfigMap = (Map<String, Object>) entry.getValue();

                McpServerConfig serverConfig = objectMapper.convertValue(serverConfigMap, McpServerConfig.class);
                serverConfigs.put(serverName, serverConfig);
                log.info("Loaded MCP server config: {}", serverName);
            }

            return serverConfigs;
        }
    }

    /**
     * Read specific MCP server configuration by name
     */
    public McpServerConfig readConfig(String serverName) throws IOException {
        Map<String, McpServerConfig> allConfigs = readAllConfigs();

        if (!allConfigs.containsKey(serverName)) {
            throw new IOException(serverName + " configuration not found in mcp-servers.json");
        }

        return allConfigs.get(serverName);
    }

    /**
     * Read Amap Maps configuration (for backward compatibility)
     */
    public McpServerConfig readAmapConfig() throws IOException {
        return readConfig("amap-maps");
    }
}
