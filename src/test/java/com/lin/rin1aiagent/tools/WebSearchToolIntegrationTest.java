package com.lin.rin1aiagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TAVILY_API_KEY", matches = ".+")
class WebSearchToolIntegrationTest {

    @Autowired
    private WebSearchTool webSearchTool;

    @Test
    void testRealSearch() {
        String result = webSearchTool.search("Spring Boot 3.5 新特性");

        assertNotNull(result);
        assertTrue(result.contains("搜索结果"));
        assertTrue(result.contains("链接"));
        assertTrue(result.contains("摘要"));

        System.out.println("搜索结果:");
        System.out.println(result);
    }

    @Test
    void testSearchWithEnglishQuery() {
        String result = webSearchTool.search("What is Spring AI");

        assertNotNull(result);
        assertTrue(result.contains("搜索结果") || result.contains("未找到"));
    }
}
