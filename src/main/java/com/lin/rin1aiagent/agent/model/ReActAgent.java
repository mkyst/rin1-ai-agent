package com.lin.rin1aiagent.agent.model;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct Agent（Reasoning + Acting）
 *
 * <p>继承自 {@link ToolCallAgent}，在工具调用能力的基础上，通过在系统提示词中注入
 * ReAct 格式约束，引导 LLM 按照"思考 → 行动 → 观察"的结构化方式解决问题。
 *
 * <p>ReAct 模式每步输出格式：
 * <pre>
 *   Thought:     分析当前状况，决定下一步行动
 *   Action:      调用工具 或 给出最终回答
 *   Observation: 工具返回的结果（由 Spring AI 自动填充）
 *   ...（循环直到给出 Final Answer）
 *   Final Answer: 最终回答
 * </pre>
 *
 * <p>终止条件：LLM 输出包含 {@code "Final Answer:"} 或父类判断无更多工具调用。
 *
 * <p>使用示例：
 * <pre>{@code
 * ReActAgent agent = new ReActAgent(dashscopeChatModel);
 * agent.setSystemPrompt("你是一个信息检索助手");
 * agent.addTool(searchTool, calculatorTool);
 * String result = agent.run("2024年中国GDP是多少？比上年增长了多少？");
 * }</pre>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ReActAgent extends ToolCallAgent {

    /**
     * ReAct 格式约束提示词，追加到用户自定义系统提示词之后。
     * 使用文本块（Text Block）保持可读性。
     */
    private static final String REACT_PROMPT_SUFFIX = """

        你是一个使用 ReAct（推理+行动）模式的智能助手。
        每一步请严格按照以下格式思考和行动：

        Thought: 分析当前状况，思考下一步该做什么
        Action: 选择并调用合适的工具，或者给出最终回答
        Observation: 观察工具返回的结果

        当你认为已经收集到足够信息可以回答用户问题时，请直接给出最终回答，以 "Final Answer:" 开头。
        """;

    /**
     * @param chatModel DashScope 聊天模型，由 Spring 容器注入
     */
    public ReActAgent(DashScopeChatModel chatModel) {
        super(chatModel);
        setName("ReActAgent");
    }

    /**
     * 在父类工具调用逻辑之前，临时将 ReAct 提示词追加到系统提示中，
     * 执行完毕后恢复原始提示词，避免污染父类状态。
     *
     * @param userPrompt 用户任务描述
     * @return LLM 的结构化回复（包含 Thought/Action/Observation 或 Final Answer）
     */
    @Override
    protected String step(String userPrompt) {
        String originalPrompt = getSystemPrompt();
        setSystemPrompt(originalPrompt + REACT_PROMPT_SUFFIX);
        String result = super.step(userPrompt);
        // 执行后恢复，防止多步循环中提示词重复叠加
        setSystemPrompt(originalPrompt);
        return result;
    }

    /**
     * 检测到 "Final Answer:" 标记时终止，或由父类（无工具调用）触发终止。
     *
     * @param stepResult 本步骤 LLM 输出
     * @return true 表示任务完成
     */
    @Override
    protected boolean shouldFinish(String stepResult) {
        if (stepResult != null && stepResult.contains("Final Answer:")) {
            return true;
        }
        return super.shouldFinish(stepResult);
    }
}
