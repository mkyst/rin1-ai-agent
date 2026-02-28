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
}