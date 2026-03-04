package com.lin.rin1aiagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 终止工具
 * 让 Agent 在任务完成或无法继续时主动终止执行循环，
 * 避免无意义地消耗步数直到 maxSteps 上限。
 */
@Slf4j
public class TerminateTool {

    /** Agent 调用此工具后，外部通过 isTerminated() 检测并终止循环 */
    private volatile boolean terminated = false;
    private String terminateReason = "";

    @Tool(description = "Terminate the agent execution. Call this when the task is completed or " +
            "when you determine the task cannot be completed. Provide a clear reason.")
    public String terminate(
            @ToolParam(description = "Reason for termination, e.g. 'Task completed' or 'Unable to proceed'")
            String reason) {
        this.terminated = true;
        this.terminateReason = reason;
        log.info("Agent 主动终止，原因: {}", reason);
        return "TERMINATE: " + reason;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public String getTerminateReason() {
        return terminateReason;
    }

    /** 重置状态，复用同一实例时调用 */
    public void reset() {
        this.terminated = false;
        this.terminateReason = "";
    }
}
