package com.lin.rin1aiagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * 终端命令执行工具
 * 允许 Agent 在本机执行 shell 命令并获取输出结果
 */
@Slf4j
public class TerminalOperationTool {

    /** 命令执行超时时间（秒） */
    private static final int TIMEOUT_SECONDS = 30;
    /** 输出内容最大长度，防止超长输出撑爆上下文 */
    private static final int MAX_OUTPUT_LENGTH = 3000;

    /**
     * 执行终端命令
     *
     * @param command 要执行的 shell 命令，如 "ls -la" 或 "echo hello"
     * @return 命令的标准输出和标准错误，执行失败时返回错误信息
     */
    @Tool(description = "Execute a terminal/shell command on the local machine and return the output. " +
            "Use this to run system commands, scripts, or CLI tools.")
    public String execute(@ToolParam(description = "The shell command to execute, e.g. 'ls -la' or 'python script.py'") String command) {
        log.info("执行命令: {}", command);
        try {
            // 根据操作系统选择 shell
            String[] shell = isWindows()
                    ? new String[]{"cmd.exe", "/c", command}
                    : new String[]{"/bin/sh", "-c", command};

            Process process = new ProcessBuilder(shell)
                    .redirectErrorStream(true)  // 合并 stderr 到 stdout
                    .start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (output.length() > MAX_OUTPUT_LENGTH) {
                        output.append("...[输出过长，已截断]");
                        break;
                    }
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "命令执行超时（超过 " + TIMEOUT_SECONDS + " 秒）";
            }

            int exitCode = process.exitValue();
            String result = output.toString().trim();
            log.info("命令退出码: {}", exitCode);

            if (exitCode != 0 && result.isEmpty()) {
                return "命令执行失败，退出码: " + exitCode;
            }
            return result.isEmpty() ? "命令执行成功（无输出）" : result;

        } catch (Exception e) {
            log.error("命令执行异常: {}", command, e);
            return "执行失败: " + e.getMessage();
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
