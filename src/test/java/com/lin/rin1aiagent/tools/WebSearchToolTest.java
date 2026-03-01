package com.lin.rin1aiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebSearchToolTest {

    @Autowired
    private WebSearchTool webSearchTool;

    @Test
    void testSearchReturnsResults() {
        String result = webSearchTool.search("Java Spring Boot");
        assertNotNull(result);
        assertTrue(result.contains("搜索结果"));
    }

    @Test
    void testSearchWithEmptyQuery() {
        String result = webSearchTool.search("");
        assertTrue(result.contains("搜索失败") || result.contains("查询不能为空"));
    }

    @Test
    void testSearchWithNullQuery() {
        String result = webSearchTool.search(null);
        assertTrue(result.contains("搜索失败") || result.contains("查询不能为空"));
    }
}
