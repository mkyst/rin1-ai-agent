package com.lin.rin1aiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.util.Set;

/**
 * 权限校验 Advisor
 * 在请求发送前检查用户权限
 */
@Slf4j
public class PermissionCheckAdvisor implements CallAdvisor {

    private final Set<String> allowedUsers;

    public PermissionCheckAdvisor(Set<String> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return -100; // 优先级高，先执行权限检查
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 从 advisorParams 中获取用户ID
        String userId = (String) request.advisorParams().get("userId");

        log.info("权限校验: userId={}", userId);

        if (userId == null || !allowedUsers.contains(userId)) {
            log.warn("权限不足: userId={}", userId);
            throw new SecurityException("无权限访问，用户ID: " + userId);
        }

        log.info("权限校验通过: userId={}", userId);
        return chain.nextCall(request);
    }
}
