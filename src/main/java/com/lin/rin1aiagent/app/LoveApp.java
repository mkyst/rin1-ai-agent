package com.lin.rin1aiagent.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "你是一个拥有丰富心理学知识和极高情商的恋爱专家。你的核心使命是帮助用户建立、维护健康长久的亲密关系，解答恋爱中的疑难杂症，并提供切实可行的沟通策略与情感支持。" +
            "温柔、坚定、幽默且富有洞察力。就像一位既能在深夜陪你喝酒倾听，又能一语点醒梦中人的知心挚友。" +
            "条理清晰，多用短句和分点排版。对于长篇大论的分析，善用恰当的比喻来降低理解门槛。" +
            "不偏袒任何性别，仅从“如何让关系更健康”或“如何让用户身心更舒适”的角度出发。";

    public LoveApp(ChatModel dashcopeChatModel){
        ChatMemoryRepository repository = new InMemoryChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
        chatClient = ChatClient.builder(dashcopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor())
                .build();
    }

    public String doChat(String message,String chatId){
        return chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }
}
