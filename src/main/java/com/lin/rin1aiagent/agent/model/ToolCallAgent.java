package com.lin.rin1aiagent.agent.model;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.lin.rin1aiagent.tools.TerminateTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具调用 Agent
 *
 * <p>继承自 {@link BaseAgent}，在单步执行中通过 Spring AI ChatClient 向 DashScope 模型
 * 发送请求，并自动处理工具调用（Function Calling）。
 *
 * <p>工作流程：
 * <pre>
 *   step()
 *     ├── 构建 ChatClient 请求（携带注册的工具列表）
 *     ├── LLM 决策：直接回答 or 调用工具
 *     │     ├── 调用工具 → Spring AI 自动执行 → 结果回传 LLM → 继续对话
 *     │     └── 直接回答 → finished = true → 下一步 shouldFinish() 返回 true
 *     └── 返回本步输出
 * </pre>
 *
 * <p>使用示例：
 * <pre>{@code
 * ToolCallAgent agent = new ToolCallAgent(dashscopeChatModel);
 * agent.setSystemPrompt("你是一个天气查询助手");
 * agent.addTool(weatherTool);
 * String result = agent.run("北京今天天气怎么样？");
 * }</pre>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolCallAgent extends BaseAgent {

    /** Spring AI ChatClient，封装与 LLM 的交互 */
    private final ChatClient chatClient;

    /** 注册的工具列表，LLM 可从中选择调用 */
    private final List<ToolCallback> tools = new ArrayList<>();

    /** 标记当前任务是否已完成（LLM 给出最终回答时置为 true） */
    private boolean finished = false;

    /** 终止工具，Agent 可主动调用以中断执行循环 */
    private final TerminateTool terminateTool = new TerminateTool();

    /**
     * @param chatModel DashScope 聊天模型，由 Spring 容器注入
     */
    public ToolCallAgent(DashScopeChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        setName("ToolCallAgent");
        // 默认注册终止工具
        // 注意：ToolCallback 的创建方式依赖于 Spring AI 版本
        // 如果需要使用终止工具，请通过 addTool() 方法手动添加
    }

    /**
     * 注册工具到当前 Agent。
     *
     * @param toolCallbacks 一个或多个工具回调，LLM 可按需调用
     */
    public void addTool(ToolCallback... toolCallbacks) {
        for (ToolCallback cb : toolCallbacks) {
            tools.add(cb);
        }
    }

    /** 每次 run 前重置终止状态，支持同一实例多次调用 */
    @Override
    public String run(String userPrompt) {
        finished = false;
        terminateTool.reset();
        return super.run(userPrompt);
    }

    /**
     * 单步执行：向 LLM 发送请求，自动处理工具调用。
     *
     * <p>{@code internalToolExecutionEnabled(true)} 让 Spring AI 在内部自动完成
     * "调用工具 → 获取结果 → 回传 LLM" 的循环，无需手动处理。
     *
     * @param userPrompt 用户任务描述
     * @return LLM 的文本回复（工具调用结果已被 LLM 消化后的最终输出）
     */
    @Override
    protected String step(String userPrompt) {
        String systemPrompt = getSystemPrompt();
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .user(userPrompt)
                .toolCallbacks(tools.toArray(new ToolCallback[0]))
                .options(ToolCallingChatOptions.builder()
                        .internalToolExecutionEnabled(true)
                        .build());
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            spec = spec.system(systemPrompt);
        }
        ChatResponse response = spec.call().chatResponse();

        Generation result = response.getResult();
        String text = result.getOutput().getText();

        // 没有待执行的工具调用，说明 LLM 已给出最终回答，标记完成
        if (result.getOutput().getToolCalls() == null || result.getOutput().getToolCalls().isEmpty()) {
            finished = true;
        }

        log.info("[{}] LLM 回复: {}", getName(), text);
        return text != null ? text : "";
    }

    /**
     * 当 LLM 不再发起工具调用、或主动调用了 terminate 工具时终止循环。
     */
    @Override
    protected boolean shouldFinish(String stepResult) {
        if (terminateTool.isTerminated()) {
            log.info("[{}] 收到终止信号: {}", getName(), terminateTool.getTerminateReason());
            return true;
        }
        return finished;
    }
}
