package com.lin.rin1aiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Set;

/**
 * 违禁词校验 Advisor
 * 检查用户输入是否包含违禁词
 */
@Slf4j
public class SensitiveWordAdvisor implements CallAdvisor {

    private final Set<String> sensitiveWords;

    public SensitiveWordAdvisor(Set<String> sensitiveWords) {
        this.sensitiveWords = sensitiveWords;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return -90; // 在权限检查之后执行
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 检查用户消息中的违禁词
        for (Message message : request.messages()) {
            if (message instanceof UserMessage userMessage) {
                String content = userMessage.getContent();

                for (String word : sensitiveWords) {
                    if (content.contains(word)) {
                        log.warn("检测到违禁词: {}, 消息内容: {}", word, content);
                        throw new IllegalArgumentException("消息包含违禁词: " + word);
                    }
                }
            }
        }

        log.info("违禁词校验通过");
        return chain.nextCall(request);
    }
}
