package com.lin.rin1aiagent.tools;

import com.lin.rin1aiagent.dto.TavilySearchResponse;
import com.lin.rin1aiagent.dto.TavilySearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSearchToolTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WebSearchTool webSearchTool;

    @BeforeEach
    void setUp() {
        // 注入配置值
        ReflectionTestUtils.setField(webSearchTool, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(webSearchTool, "apiUrl", "https://api.tavily.com/search");
    }

    @Test
    void testSearchReturnsResults() {
        // 准备模拟响应
        TavilySearchResponse mockResponse = new TavilySearchResponse();
        mockResponse.setQuery("Java Spring Boot");

        TavilySearchResult result1 = new TavilySearchResult();
        result1.setTitle("Spring Boot 官方文档");
        result1.setUrl("https://spring.io/projects/spring-boot");
        result1.setContent("Spring Boot makes it easy to create stand-alone applications");

        TavilySearchResult result2 = new TavilySearchResult();
        result2.setTitle("Spring Boot 教程");
        result2.setUrl("https://example.com/spring-boot-tutorial");
        result2.setContent("Learn Spring Boot from scratch");

        mockResponse.setResults(Arrays.asList(result1, result2));

        // 模拟 RestTemplate 调用
        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(TavilySearchResponse.class)
        )).thenReturn(mockResponse);

        // 执行测试
        String result = webSearchTool.search("Java Spring Boot");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains("搜索结果"));
        assertTrue(result.contains("Spring Boot 官方文档"));
        assertTrue(result.contains("https://spring.io/projects/spring-boot"));
        assertTrue(result.contains("Spring Boot 教程"));
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

    @Test
    void testSearchWithLongQuery() {
        // 创建一个超过400字符的查询
        String longQuery = "a".repeat(401);

        String result = webSearchTool.search(longQuery);

        assertTrue(result.contains("搜索失败"));
        assertTrue(result.contains("查询长度不能超过400个字符"));
    }

    @Test
    void testSearchWithEmptyResults() {
        // 准备空结果响应
        TavilySearchResponse mockResponse = new TavilySearchResponse();
        mockResponse.setQuery("nonexistent query");
        mockResponse.setResults(Collections.emptyList());

        // 模拟 RestTemplate 调用
        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(TavilySearchResponse.class)
        )).thenReturn(mockResponse);

        // 执行测试
        String result = webSearchTool.search("nonexistent query");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains("未找到相关搜索结果"));
    }

    @Test
    void testSearchWithApiException() {
        // 模拟 API 异常
        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(TavilySearchResponse.class)
        )).thenThrow(new RuntimeException("API connection failed"));

        // 执行测试
        String result = webSearchTool.search("test query");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains("搜索失败"));
    }
}
