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
