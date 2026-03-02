# Web Search Tool Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add web search capability to AI Agent using Tavily REST API

**Architecture:** Create WebSearchTool class that calls Tavily REST API using Spring's RestTemplate, formats results for AI consumption, with proper error handling and configuration management.

**Tech Stack:** Spring Boot, RestTemplate, Tavily REST API, Jackson for JSON parsing

---

## Task 1: Add Configuration

**Files:**
- Modify: `src/main/resources/application.yml`

**Step 1: Add Tavily API configuration**

Add to application.yml:
```yaml
tavily:
  api-key: ${TAVILY_API_KEY}
  api-url: https://api.tavily.com/search
```

**Step 2: Verify configuration format**

Check that the YAML is properly indented and follows existing patterns in the file.

**Step 3: Commit configuration**

```bash
git add src/main/resources/application.yml
git commit -m "config: add Tavily API configuration"
```

---

## Task 2: Create DTO Classes

**Files:**
- Create: `src/main/java/com/lin/rin1aiagent/dto/TavilySearchRequest.java`
- Create: `src/main/java/com/lin/rin1aiagent/dto/TavilySearchResponse.java`
- Create: `src/main/java/com/lin/rin1aiagent/dto/TavilySearchResult.java`

**Step 1: Create TavilySearchRequest DTO**

```java
package com.lin.rin1aiagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TavilySearchRequest {
    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("query")
    private String query;

    @JsonProperty("max_results")
    private Integer maxResults = 5;
}
```

**Step 2: Create TavilySearchResult DTO**

```java
package com.lin.rin1aiagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TavilySearchResult {
    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("content")
    private String content;

    @JsonProperty("score")
    private Double score;
}
```

**Step 3: Create TavilySearchResponse DTO**

```java
package com.lin.rin1aiagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class TavilySearchResponse {
    @JsonProperty("query")
    private String query;

    @JsonProperty("results")
    private List<TavilySearchResult> results;

    @JsonProperty("answer")
    private String answer;
}
```

**Step 4: Commit DTO classes**

```bash
git add src/main/java/com/lin/rin1aiagent/dto/
git commit -m "feat: add Tavily API DTO classes"
```

---

## Task 3: Write WebSearchTool Test (TDD)

**Files:**
- Create: `src/test/java/com/lin/rin1aiagent/tools/WebSearchToolTest.java`

**Step 1: Write basic search test**

```java
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
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=WebSearchToolTest`
Expected: Compilation error - WebSearchTool class not found

**Step 3: Commit test**

```bash
git add src/test/java/com/lin/rin1aiagent/tools/WebSearchToolTest.java
git commit -m "test: add WebSearchTool tests (TDD)"
```

---

## Task 4: Implement WebSearchTool

**Files:**
- Create: `src/main/java/com/lin/rin1aiagent/tools/WebSearchTool.java`

**Step 1: Create WebSearchTool class skeleton**

```java
package com.lin.rin1aiagent.tools;

import cn.hutool.core.util.StrUtil;
import com.lin.rin1aiagent.dto.TavilySearchRequest;
import com.lin.rin1aiagent.dto.TavilySearchResponse;
import com.lin.rin1aiagent.dto.TavilySearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 网页搜索工具类（使用 Tavily API）
 */
@Slf4j
@Component
public class WebSearchTool {

    @Value("${tavily.api-key}")
    private String apiKey;

    @Value("${tavily.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool(description = "Search the web for information using Tavily API")
    public String search(@ToolParam(description = "Search query") String query) {
        // Implementation will be added
        return null;
    }
}
```

**Step 2: Implement parameter validation**

Add to search method:
```java
// 参数验证
if (StrUtil.isBlank(query)) {
    return "搜索失败: 查询不能为空";
}

if (query.length() > 400) {
    return "搜索失败: 查询长度不能超过400个字符";
}
```

**Step 3: Implement API call logic**

Add after validation:
```java
try {
    // 构建请求
    TavilySearchRequest request = new TavilySearchRequest();
    request.setApiKey(apiKey);
    request.setQuery(query);
    request.setMaxResults(5);

    // 设置请求头
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<TavilySearchRequest> entity = new HttpEntity<>(request, headers);

    // 调用 API
    TavilySearchResponse response = restTemplate.postForObject(
        apiUrl,
        entity,
        TavilySearchResponse.class
    );

    // 格式化结果
    return formatSearchResults(response);

} catch (Exception e) {
    log.error("搜索失败: {}", e.getMessage(), e);
    return "搜索失败: " + e.getMessage();
}
```

**Step 4: Implement result formatting**

Add private method:
```java
private String formatSearchResults(TavilySearchResponse response) {
    if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
        return "未找到相关搜索结果";
    }

    StringBuilder result = new StringBuilder("搜索结果：\n\n");
    List<TavilySearchResult> results = response.getResults();

    for (int i = 0; i < results.size(); i++) {
        TavilySearchResult item = results.get(i);
        result.append(i + 1).append(". ").append(item.getTitle()).append("\n");
        result.append("   链接: ").append(item.getUrl()).append("\n");
        result.append("   摘要: ").append(item.getContent()).append("\n\n");
    }

    return result.toString();
}
```

**Step 5: Run tests to verify implementation**

Run: `mvn test -Dtest=WebSearchToolTest`
Expected: Tests should pass (requires TAVILY_API_KEY environment variable)

**Step 6: Commit implementation**

```bash
git add src/main/java/com/lin/rin1aiagent/tools/WebSearchTool.java
git commit -m "feat: implement WebSearchTool with Tavily API"
```

---

## Task 5: Integration Testing

**Files:**
- Create: `src/test/java/com/lin/rin1aiagent/tools/WebSearchToolIntegrationTest.java`

**Step 1: Write integration test**

```java
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
```

**Step 2: Run integration test**

Run: `TAVILY_API_KEY=your_key mvn test -Dtest=WebSearchToolIntegrationTest`
Expected: Real API calls succeed and return formatted results

**Step 3: Commit integration test**

```bash
git add src/test/java/com/lin/rin1aiagent/tools/WebSearchToolIntegrationTest.java
git commit -m "test: add WebSearchTool integration tests"
```

---

## Task 6: Update Documentation

**Files:**
- Create: `docs/tools/web-search-tool.md`

**Step 1: Write tool documentation**

```markdown
# WebSearchTool 使用文档

## 概述

WebSearchTool 提供网页搜索功能，使 AI Agent 能够获取实时网络信息。

## 配置

### 环境变量

```bash
export TAVILY_API_KEY=your_tavily_api_key
```

### application.yml

```yaml
tavily:
  api-key: ${TAVILY_API_KEY}
  api-url: https://api.tavily.com/search
```

## 使用方法

### 在 AI Agent 中使用

```java
@Bean
public ChatClient chatClient(ChatClient.Builder builder, WebSearchTool webSearchTool) {
    return builder
        .defaultTools(webSearchTool)
        .build();
}
```

### 直接调用

```java
@Autowired
private WebSearchTool webSearchTool;

public void search() {
    String result = webSearchTool.search("Java Spring Boot");
    System.out.println(result);
}
```

## 返回格式

```
搜索结果：

1. [标题]
   链接: [URL]
   摘要: [内容摘要]

2. [标题]
   链接: [URL]
   摘要: [内容摘要]
```

## 错误处理

- 查询为空: 返回 "搜索失败: 查询不能为空"
- 查询过长: 返回 "搜索失败: 查询长度不能超过400个字符"
- API 错误: 返回 "搜索失败: [错误信息]"
- 无结果: 返回 "未找到相关搜索结果"

## 限制

- 每次搜索返回最多 5 条结果
- 查询长度限制 400 字符
- 依赖 Tavily API 配额限制

## 获取 API Key

1. 访问 https://tavily.com
2. 注册账号
3. 获取免费 API key（每月 1000 次免费调用）
```

**Step 2: Commit documentation**

```bash
git add docs/tools/web-search-tool.md
git commit -m "docs: add WebSearchTool documentation"
```

---

## Task 7: Verify Complete Implementation

**Step 1: Run all tests**

Run: `mvn clean test`
Expected: All tests pass

**Step 2: Build project**

Run: `mvn clean package`
Expected: Build succeeds without errors

**Step 3: Manual verification**

1. Set TAVILY_API_KEY environment variable
2. Run application: `mvn spring-boot:run`
3. Test search functionality through AI Agent

**Step 4: Final commit**

```bash
git add .
git commit -m "feat: complete WebSearchTool implementation

- Add Tavily API integration
- Implement search with error handling
- Add comprehensive tests
- Add documentation

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Completion Checklist

- [ ] Configuration added to application.yml
- [ ] DTO classes created for API communication
- [ ] WebSearchTool implemented with @Tool annotation
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] Documentation created
- [ ] All tests pass
- [ ] Build succeeds
- [ ] Manual testing completed

## Notes

- Tavily API key required for testing and runtime
- Free tier: 1000 API calls per month
- Results limited to 5 per search for optimal AI consumption
- Error messages in Chinese for consistency with existing code