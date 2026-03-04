package com.lin.rin1aiagent.agent.model;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class AgentTest {

    @Resource
    private DashScopeChatModel dashScopeChatModel;

    @Test
    void testToolCallAgent() {
        ToolCallAgent agent = new ToolCallAgent(dashScopeChatModel);
        agent.setSystemPrompt("你是一个助手，请直接回答问题。");
        String result = agent.run("用一句话介绍Spring AI");
        System.out.println("ToolCallAgent result: " + result);
        assertThat(result).isNotBlank();
    }

    @Test
    void testReActAgent() {
        ReActAgent agent = new ReActAgent(dashScopeChatModel);
        agent.setSystemPrompt("你是一个助手，请直接回答问题。");
        String result = agent.run("1+1等于几？请给出 Final Answer。");
        System.out.println("ReActAgent result: " + result);
        assertThat(result).isNotBlank();
    }

    @Test
    void testRinManus() {
        RinManus manus = new RinManus(dashScopeChatModel);
        manus.registerAgent("问答Agent", "负责回答通用知识问题");
        manus.registerAgent("翻译Agent", "负责将文本翻译成英文");
        String result = manus.run("用一句话介绍Java，然后把这句话翻译成英文");
        System.out.println("RinManus result: " + result);
        assertThat(result).isNotBlank();
    }
}
