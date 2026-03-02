package com.lin.rin1aiagent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lin.rin1aiagent.mcp.model.McpServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP 服务器配置读取器
 *
 * 功能：
 * - 从 classpath 下的 mcp-servers.json 读取 MCP 服务器配置
 * - 支持读取所有服务器配置或指定服务器配置
 * - 将 JSON 配置转换为 Java 对象
 *
 * 配置文件格式示例：
 * <pre>
 * {
 *   "mcpServers": {
 *     "amap-maps": {
 *       "command": "npx.cmd",
 *       "args": ["-y", "@amap/amap-maps-mcp-server"],
 *       "env": {
 *         "AMAP_MAPS_API_KEY": "your-api-key"
 *       }
 *     },
 *     "image-search": {
 *       "command": "java",
 *       "args": ["-jar", "image-search-server.jar"],
 *       "env": {}
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Claude
 * @since 1.0
 */
@Slf4j
public class McpConfigReader {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 读取所有 MCP 服务器配置
     *
     * 工作流程：
     * 1. 从 classpath 加载 mcp-servers.json 文件
     * 2. 解析 JSON 获取 mcpServers 对象
     * 3. 遍历所有服务器配置，转换为 McpServerConfig 对象
     * 4. 返回服务器名称到配置对象的映射
     *
     * @return Map<服务器名称, 服务器配置对象>
     * @throws IOException 如果文件不存在或解析失败
     */
    public Map<String, McpServerConfig> readAllConfigs() throws IOException {
        ClassPathResource resource = new ClassPathResource("mcp-servers.json");

        try (InputStream inputStream = resource.getInputStream()) {
            // 解析 JSON 文件
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(inputStream, Map.class);

            // 获取 mcpServers 节点
            @SuppressWarnings("unchecked")
            Map<String, Object> mcpServers = (Map<String, Object>) config.get("mcpServers");

            // 检查是否有配置
            if (mcpServers == null || mcpServers.isEmpty()) {
                log.warn("No MCP servers found in mcp-servers.json");
                return new HashMap<>();
            }

            // 转换所有服务器配置
            Map<String, McpServerConfig> serverConfigs = new HashMap<>();
            for (Map.Entry<String, Object> entry : mcpServers.entrySet()) {
                String serverName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> serverConfigMap = (Map<String, Object>) entry.getValue();

                // 将 Map 转换为 McpServerConfig 对象
                McpServerConfig serverConfig = objectMapper.convertValue(serverConfigMap, McpServerConfig.class);
                serverConfigs.put(serverName, serverConfig);
                log.info("Loaded MCP server config: {}", serverName);
            }

            return serverConfigs;
        }
    }

    /**
     * 读取指定名称的 MCP 服务器配置
     *
     * @param serverName 服务器名称（如 "amap-maps"）
     * @return 服务器配置对象
     * @throws IOException 如果服务器配置不存在或读取失败
     */
    public McpServerConfig readConfig(String serverName) throws IOException {
        Map<String, McpServerConfig> allConfigs = readAllConfigs();

        if (!allConfigs.containsKey(serverName)) {
            throw new IOException(serverName + " configuration not found in mcp-servers.json");
        }

        return allConfigs.get(serverName);
    }

    /**
     * 读取高德地图 MCP 服务器配置
     *
     * 这是一个便捷方法，用于向后兼容
     *
     * @return 高德地图服务器配置
     * @throws IOException 如果配置不存在或读取失败
     */
    public McpServerConfig readAmapConfig() throws IOException {
        return readConfig("amap-maps");
    }
}
