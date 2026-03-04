package com.lin.rin1aiagent.agent.model;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.*;

/**
 * RinManus - 多 Agent 编排器
 *
 * <p>由一个"规划 LLM"将用户任务分解为子任务列表，再将每个子任务分发给
 * 最合适的子 Agent 执行，最终汇总所有结果返回。
 *
 * <p>工作流程：
 * <pre>
 *   step(userPrompt)
 *     ├── 1. 构建已注册 Agent 的描述列表
 *     ├── 2. 规划 LLM 输出 JSON 任务分解计划
 *     ├── 3. 按顺序分发给对应子 Agent 执行
 *     └── 4. 拼接所有子 Agent 输出返回
 * </pre>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class RinManus extends BaseAgent {

    /** 规划 LLM 客户端，专门负责任务分解，与子 Agent 执行客户端相互独立 */
    private final ChatClient plannerClient;

    /** DashScope 模型，用于动态创建子 Agent */
    private final DashScopeChatModel chatModel;

    /**
     * 已注册的子 Agent 映射表。
     * key: Agent 名称（需与规划 LLM 输出的 "agent" 字段一致）
     * 使用 LinkedHashMap 保持注册顺序，便于规划 LLM 按顺序读取描述
     */
    private final Map<String, ToolCallAgent> agents = new LinkedHashMap<>();

    /** 标记编排任务是否完成（单步执行完毕即完成） */
    private boolean finished = false;

    /**
     * 规划提示词模板，%s 替换为已注册 Agent 的描述列表。
     * 要求 LLM 严格输出 JSON 数组，便于程序解析。
     */
    private static final String PLANNER_PROMPT = """
            你是一个任务规划专家。根据用户的需求，将任务分解为多个子任务，并为每个子任务指定最合适的 Agent 来执行。

            可用的 Agent 列表：
            %s

            请按以下 JSON 格式输出任务计划（不要输出其他内容）：
            [{"agent": "agent名称", "task": "具体任务描述"}]

            如果任务很简单不需要分解，直接输出一个元素的数组即可。
            """;

    /**
     * @param chatModel DashScope 聊天模型，由 Spring 容器注入
     */
    public RinManus(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        this.plannerClient = ChatClient.builder(chatModel).build();
        setName("RinManus");
    }

    /**
     * 注册子 Agent（内部自动创建 ToolCallAgent 实例）。
     *
     * @param name        Agent 名称，需与规划 LLM 输出一致
     * @param description Agent 功能描述，规划 LLM 依据此分配任务
     * @param tools       该 Agent 可使用的工具
     */
    public void registerAgent(String name, String description, ToolCallback... tools) {
        ToolCallAgent agent = new ToolCallAgent(chatModel);
        agent.setName(name);
        agent.setDescription(description);
        agent.addTool(tools);
        agents.put(name, agent);
    }

    /**
     * 注册已有的 ToolCallAgent 实例（适用于需要自定义配置的场景）。
     */
    public void registerAgent(ToolCallAgent agent) {
        agents.put(agent.getName(), agent);
    }

    /**
     * 编排执行：规划 → 分发 → 汇总。
     *
     * @return 所有子 Agent 结果拼接，格式为 "[AgentName]: 结果\n"
     */
    @Override
    protected String step(String userPrompt) {
        // 1. 构建可用 Agent 描述
        StringBuilder agentDesc = new StringBuilder();
        agents.forEach((name, agent) ->
                agentDesc.append(String.format("- %s: %s\n", name, agent.getDescription()))
        );

        // 2. 让规划 LLM 分解任务
        String plan = plannerClient.prompt()
                .system(String.format(PLANNER_PROMPT, agentDesc))
                .user(userPrompt)
                .call()
                .content();

        log.info("[{}] 任务计划: {}", getName(), plan);

        // 3. 解析并执行计划
        StringBuilder result = new StringBuilder();
        List<Map<String, String>> tasks = parsePlan(plan);

        for (Map<String, String> task : tasks) {
            String agentName = task.get("agent");
            String taskDesc = task.get("task");
            ToolCallAgent agent = agents.get(agentName);

            if (agent == null) {
                log.warn("未找到 Agent: {}，跳过任务: {}", agentName, taskDesc);
                continue;
            }

            log.info("[{}] 分配任务给 {}: {}", getName(), agentName, taskDesc);
            String taskResult = agent.run(taskDesc);
            result.append(String.format("[%s]: %s\n", agentName, taskResult));
        }

        finished = true;
        return result.toString();
    }

    /** 编排器单步即完成 */
    @Override
    protected boolean shouldFinish(String stepResult) {
        return finished;
    }

    /**
     * 解析规划 LLM 输出的 JSON 任务计划。
     * 容错：解析失败时降级为单 Agent 执行整个任务。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parsePlan(String plan) {
        try {
            // 提取 JSON 数组部分
            int start = plan.indexOf('[');
            int end = plan.lastIndexOf(']');
            if (start == -1 || end == -1) {
                return List.of(Map.of("agent", agents.keySet().iterator().next(), "task", plan));
            }
            String json = plan.substring(start, end + 1);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, List.class);
        } catch (Exception e) {
            log.warn("解析任务计划失败，使用第一个 Agent 执行整个任务", e);
            return List.of(Map.of("agent", agents.keySet().iterator().next(), "task", plan));
        }
    }
}
