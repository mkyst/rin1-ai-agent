package com.lin.rin1aiagent.app;

import com.lin.rin1aiagent.advisor.PermissionCheckAdvisor;
import com.lin.rin1aiagent.advisor.SensitiveWordAdvisor;
import com.lin.rin1aiagent.advisor.SimpleLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 带权限和违禁词校验的聊天应用示例
 */
@Component
@Slf4j
public class SecureChatApp {

    private final ChatClient chatClient;

    public SecureChatApp(ChatModel dashscopeChatModel) {
        // 配置允许的用户列表
        Set<String> allowedUsers = Set.of("user001", "user002", "admin");

        // 配置违禁词列表
        Set<String> sensitiveWords = Set.of("暴力", "色情", "赌博");

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("你是一个友好的AI助手")
                .defaultAdvisors(
                        new PermissionCheckAdvisor(allowedUsers),    // 权限校验
                        new SensitiveWordAdvisor(sensitiveWords),    // 违禁词校验
                        new SimpleLoggerAdvisor()                     // 日志记录
                )
                .build();
    }

    /**
     * 带权限和违禁词校验的聊天
     * @param message 用户消息
     * @param userId 用户ID
     * @return AI回复
     */
    public String secureChat(String message, String userId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param("userId", userId))  // 传递用户ID用于权限校验
                .call()
                .content();
    }
}
