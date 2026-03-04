package com.lin.rin1aiagent.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.lin.rin1aiagent.agent.model.RinManus;
import com.lin.rin1aiagent.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用 AI 恋爱大师应用
     */
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用（基础版）
     */
    @GetMapping(value = "/love_app/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppStream(String message, String chatId) {
        return loveApp.doChatStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用（带 RAG）
     */
    @GetMapping(value = "/love_app/chat/stream/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppStreamRag(String message, String chatId) {
        return loveApp.doChatWithRagStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用（带 MCP 工具）
     */
    @GetMapping(value = "/love_app/chat/stream/mcp", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppStreamMcp(String message, String chatId) {
        return loveApp.doChatWithMcpStream(message, chatId);
    }

    /**
     * 流式调用 Manus 超级智能体
     */
    @GetMapping(value = "/manus/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithManusStream(String message) {
        RinManus manus = new RinManus((DashScopeChatModel) dashscopeChatModel);
        manus.registerAgent("通用助手", "负责回答通用问题和执行基础任务");
        manus.registerAgent("搜索助手", "负责网络信息检索");
        return Flux.just(manus.run(message));
    }
}
