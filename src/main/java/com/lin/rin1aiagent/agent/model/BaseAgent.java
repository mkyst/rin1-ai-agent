package com.lin.rin1aiagent.agent.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent 基础抽象类
 *
 * <p>定义了所有 Agent 的通用生命周期：
 * <pre>
 *   run(userPrompt)
 *     └── while (currentStep < maxSteps)
 *           ├── step(userPrompt)   // 子类实现单步逻辑
 *           └── shouldFinish()    // 子类决定是否终止
 * </pre>
 *
 * <p>内置最大步数保护（默认 10 步），防止 Agent 陷入无限循环。
 */
@Slf4j
@Data
public abstract class BaseAgent {

    /** Agent 名称，用于日志标识 */
    private String name = "BaseAgent";

    /** Agent 功能描述，供编排器（如 RinManus）选择 Agent 时使用 */
    private String description = "";

    /** 系统提示词，注入给 LLM 的角色设定 */
    private String systemPrompt = "";

    /** 当前已执行步数 */
    private int currentStep = 0;

    /** 最大允许步数，超出后强制终止并打印警告 */
    private int maxSteps = 10;

    /**
     * 启动 Agent 执行循环。
     *
     * @param userPrompt 用户输入的原始任务描述
     * @return 所有步骤输出的拼接结果
     */
    public String run(String userPrompt) {
        currentStep = 0;
        StringBuilder result = new StringBuilder();
        while (currentStep < maxSteps) {
            currentStep++;
            log.info("[{}] 执行第 {} 步", name, currentStep);
            String stepResult = step(userPrompt);
            result.append(stepResult);
            if (shouldFinish(stepResult)) {
                log.info("[{}] 任务完成，共执行 {} 步", name, currentStep);
                break;
            }
        }
        if (currentStep >= maxSteps) {
            log.warn("[{}] 已达到最大步数限制 {}，强制终止", name, maxSteps);
        }
        return result.toString();
    }

    /**
     * 单步执行逻辑，由子类实现。
     *
     * <p>每次调用代表 Agent 的一次"思考-行动"循环。
     *
     * @param userPrompt 用户原始任务（每步都传入，子类可根据需要维护上下文）
     * @return 本步骤的输出内容
     */
    protected abstract String step(String userPrompt);

    /**
     * 判断是否应该终止循环，由子类实现。
     *
     * @param stepResult 本步骤的输出内容
     * @return true 表示任务已完成，退出循环
     */
    protected abstract boolean shouldFinish(String stepResult);
}
