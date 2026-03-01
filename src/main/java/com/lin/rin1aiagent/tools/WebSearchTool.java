package com.lin.rin1aiagent.tools;

import cn.hutool.core.util.StrUtil;
import com.lin.rin1aiagent.dto.TavilySearchRequest;
import com.lin.rin1aiagent.dto.TavilySearchResponse;
import com.lin.rin1aiagent.dto.TavilySearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "tavily.api-key")
public class WebSearchTool {

    @Value("${tavily.api-key}")
    private String apiKey;

    @Value("${tavily.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public WebSearchTool(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Tool(description = "Search the web for information using Tavily API")
    public String search(@ToolParam(description = "Search query") String query) {
        // 参数验证
        if (StrUtil.isBlank(query)) {
            return "搜索失败: 查询不能为空";
        }

        if (query.length() > 400) {
            return "搜索失败: 查询长度不能超过400个字符";
        }

        try {
            // 构建请求
            TavilySearchRequest request = TavilySearchRequest.builder()
                    .apiKey(apiKey)
                    .query(query)
                    .maxResults(5)
                    .build();

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
    }

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
}
