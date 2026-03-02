package com.lin.rin1aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveAppTest {
    @Resource
    private LoveApp loveApp;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();

        String answer1 = loveApp.doChat("你好，我是张三", chatId);
//        System.out.println("A1: " + answer1);

        String answer2 = loveApp.doChat("我想找个女朋友", chatId);
//        System.out.println("A2: " + answer2);

        String answer3 = loveApp.doChat("我刚才跟你说我叫什么来着", chatId);
//        System.out.println("A3: " + answer3);

        assertThat(answer1).isNotBlank();
        assertThat(answer2).isNotBlank();
        assertThat(answer3).contains("张三");

    }

    @Test
    void doChatWithReport(){
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是张三我和一个青梅竹马的女孩表白，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message,chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "恋爱久了没话说，正常吗？如何保持分享欲？";
        String answer = loveApp.doChatWithRag(message,chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
        String message = "我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点";
        String answer =  loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
        // 测试图片搜索 MCP
//        String message = "帮我搜索一些哄另一半开心的图片";
//        String answer =  loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }
}