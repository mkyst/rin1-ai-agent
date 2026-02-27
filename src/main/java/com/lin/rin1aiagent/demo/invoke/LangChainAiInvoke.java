package com.lin.rin1aiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class LangChainAiInvoke {

    public static void main(String[] args) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先设置环境变量 DASHSCOPE_API_KEY");
        }

        ChatLanguageModel qwenChatModel = QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-max")
                .build();

        String result = qwenChatModel.chat("你是谁？");
        System.out.println(result);
    }
}