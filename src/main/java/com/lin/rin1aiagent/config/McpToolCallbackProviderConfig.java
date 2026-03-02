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

/**
 * MCP (Model Context Protocol) 工具回调提供者配置类
 *
 * 功能：
 * 1. 从 mcp-servers.json 读取所有 MCP 服务器配置
 * 2. 初始化并启动所有 MCP 服务器进程
 * 3. 发现并注册 MCP 服务器提供的工具
 * 4. 管理 MCP 客户端的生命周期（启动和关闭）
 *
 * 使用方式：
 * - 在 src/main/resources/mcp-servers.json 中添加新的 MCP 服务器配置
 * - 重启应用即可自动加载新的 MCP 工具
 *
 * @author Claude
 * @since 1.0
 */
@Configuration
@Slf4j
public class McpToolCallbackProviderConfig {

    /**
     * 存储所有已初始化的 MCP 客户端
     * Key: MCP 服务器名称（如 "amap-maps"）
     * Value: 对应的 McpClient 实例
     */
    private final Map<String, McpClient> mcpClients = new HashMap<>();

    /**
     * 创建 ToolCallbackProvider Bean
     *
     * 工作流程：
     * 1. 读取 mcp-servers.json 中的所有服务器配置
     * 2. 遍历每个服务器配置，创建并初始化 McpClient
     * 3. 通过 McpClient 发现服务器提供的工具列表
     * 4. 记录所有工具信息到日志
     * 5. 返回 ToolCallbackProvider（目前返回空数组，工具已注册但未转换为 ToolCallback）
     *
     * 错误处理：
     * - 单个服务器初始化失败不影响其他服务器
     * - 所有服务器都失败时返回空的 ToolCallbackProvider
     *
     * @return ToolCallbackProvider 实例，提供 MCP 工具回调
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        try {
            // 读取所有 MCP 服务器配置
            McpConfigReader configReader = new McpConfigReader();
            Map<String, McpServerConfig> serverConfigs = configReader.readAllConfigs();

            // 检查是否有配置的服务器
            if (serverConfigs.isEmpty()) {
                log.warn("No MCP servers configured");
                return () -> new org.springframework.ai.tool.ToolCallback[0];
            }

            // 初始化所有 MCP 客户端
            int totalTools = 0;
            for (Map.Entry<String, McpServerConfig> entry : serverConfigs.entrySet()) {
                String serverName = entry.getKey();
                McpServerConfig serverConfig = entry.getValue();

                try {
                    log.info("Initializing MCP server: {}", serverName);

                    // 创建 MCP 客户端并初始化（启动进程、建立连接）
                    McpClient client = new McpClient(serverConfig);
                    client.initialize();

                    // 发现该服务器提供的所有工具
                    List<McpToolDefinition> tools = client.listTools();
                    log.info("Loaded {} tools from MCP server: {}", tools.size(), serverName);

                    // 记录每个工具的详细信息
                    for (McpToolDefinition tool : tools) {
                        log.info("  - {} : {}", tool.getName(), tool.getDescription());
                    }

                    // 保存客户端实例，用于后续调用和清理
                    mcpClients.put(serverName, client);
                    totalTools += tools.size();

                } catch (Exception e) {
                    // 单个服务器失败不影响其他服务器
                    log.error("Failed to initialize MCP server: {}", serverName, e);
                    // Continue with other servers
                }
            }

            log.info("Successfully initialized {} MCP servers with {} total tools",
                    mcpClients.size(), totalTools);

            // 返回空的 ToolCallbackProvider
            // 注意：工具已经被发现和注册，但目前还没有转换为 Spring AI 的 ToolCallback
            // 这是因为 MCP 工具的调用方式与 Spring AI 的 @Tool 注解方式不同
            return () -> new org.springframework.ai.tool.ToolCallback[0];

        } catch (Exception e) {
            log.error("Failed to initialize MCP tools, returning empty provider", e);
            return () -> new org.springframework.ai.tool.ToolCallback[0];
        }
    }

    /**
     * 应用关闭时的清理方法
     *
     * 功能：
     * - 遍历所有已初始化的 MCP 客户端
     * - 依次关闭每个客户端（终止进程、关闭连接）
     * - 清空客户端映射表
     *
     * 注意：
     * - 使用 @PreDestroy 注解，Spring 容器销毁时自动调用
     * - 即使某个客户端关闭失败，也会继续关闭其他客户端
     */
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down {} MCP clients", mcpClients.size());

        // 遍历所有 MCP 客户端并关闭
        for (Map.Entry<String, McpClient> entry : mcpClients.entrySet()) {
            try {
                log.info("Shutting down MCP server: {}", entry.getKey());
                entry.getValue().shutdown();
            } catch (Exception e) {
                log.error("Error shutting down MCP server: {}", entry.getKey(), e);
            }
        }

        // 清空映射表
        mcpClients.clear();
    }
}
