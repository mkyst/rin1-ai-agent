package com.lin.rin1aiagent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lin.rin1aiagent.mcp.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP (Model Context Protocol) 客户端
 *
 * 功能：
 * - 启动和管理 MCP 服务器进程
 * - 通过 stdin/stdout 与 MCP 服务器通信
 * - 实现 JSON-RPC 2.0 协议
 * - 发现和调用 MCP 服务器提供的工具
 *
 * 通信协议：
 * - 使用 JSON-RPC 2.0 格式
 * - 通过进程的标准输入输出进行通信
 * - 每个请求都有唯一的 ID
 *
 * 生命周期：
 * 1. 创建 McpClient 实例
 * 2. 调用 initialize() 启动服务器并建立连接
 * 3. 调用 listTools() 发现可用工具
 * 4. 调用 callTool() 执行工具
 * 5. 调用 shutdown() 关闭服务器进程
 *
 * @author Claude
 * @since 1.0
 */
@Slf4j
public class McpClient {
    /** MCP 服务器配置 */
    private final McpServerConfig config;

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper;

    /** MCP 服务器进程 */
    private Process process;

    /** 进程标准输入流（用于发送请求） */
    private BufferedWriter processInput;

    /** 进程标准输出流（用于接收响应） */
    private BufferedReader processOutput;

    /** 请求 ID 计数器，确保每个请求有唯一 ID */
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);

    /**
     * 构造函数
     *
     * @param config MCP 服务器配置
     */
    public McpClient(McpServerConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 初始化 MCP 客户端
     *
     * 工作流程：
     * 1. 使用 ProcessBuilder 启动 MCP 服务器进程
     * 2. 设置环境变量（如 API Key）
     * 3. 建立 stdin/stdout 通信通道
     * 4. 发送 initialize 请求进行握手
     * 5. 发送 initialized 通知完成初始化
     *
     * @throws IOException 如果进程启动失败或通信失败
     */
    public void initialize() throws IOException {
        log.info("Starting MCP server: {} {}", config.getCommand(), config.getArgs());

        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        command.add(config.getCommand());
        command.addAll(config.getArgs());
        processBuilder.command(command);

        // 设置环境变量（如 API Key）
        if (config.getEnv() != null) {
            processBuilder.environment().putAll(config.getEnv());
        }

        processBuilder.redirectErrorStream(false);

        try {
            // 启动进程
            process = processBuilder.start();
            processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // 发送 initialize 请求
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

            // 发送 initialized 通知
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

    /**
     * 发送 JSON-RPC 请求并接收响应
     *
     * 工作流程：
     * 1. 将请求对象序列化为 JSON
     * 2. 写入进程的标准输入
     * 3. 从进程的标准输出读取响应
     * 4. 将响应 JSON 反序列化为对象
     * 5. 检查是否有错误
     *
     * @param request JSON-RPC 请求对象
     * @return JSON-RPC 响应对象
     * @throws IOException 如果通信失败或服务器返回错误
     */
    private McpResponse sendRequest(McpRequest request) throws IOException {
        String requestJson = objectMapper.writeValueAsString(request);
        log.debug("Sending MCP request: {}", requestJson);

        // 发送请求
        processInput.write(requestJson);
        processInput.newLine();
        processInput.flush();

        // 接收响应
        String responseLine = processOutput.readLine();
        if (responseLine == null) {
            throw new IOException("MCP server closed connection");
        }

        log.debug("Received MCP response: {}", responseLine);
        McpResponse response = objectMapper.readValue(responseLine, McpResponse.class);

        // 检查错误
        if (response.getError() != null) {
            throw new IOException("MCP error: " + response.getError().getMessage());
        }

        return response;
    }

    /**
     * 列出 MCP 服务器提供的所有工具
     *
     * 调用 tools/list 方法获取工具列表
     *
     * @return 工具定义列表
     * @throws IOException 如果请求失败
     */
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

    /**
     * 调用 MCP 工具
     *
     * 调用 tools/call 方法执行指定的工具
     *
     * @param toolName 工具名称
     * @param arguments 工具参数（Map 格式）
     * @return 工具执行结果
     * @throws IOException 如果调用失败
     */
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

    /**
     * 关闭 MCP 客户端
     *
     * 工作流程：
     * 1. 关闭输入输出流
     * 2. 优雅地终止进程（destroy）
     * 3. 等待最多 5 秒
     * 4. 如果进程仍在运行，强制终止（destroyForcibly）
     */
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
