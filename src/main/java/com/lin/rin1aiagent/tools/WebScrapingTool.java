package com.lin.rin1aiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web scraping tool for extracting content from web pages
 */
@Component
@Slf4j
public class WebScrapingTool {

    private static final int TIMEOUT = 10000; // 10 seconds
    private static final int MAX_CONTENT_LENGTH = 5000; // Limit content length

    @Tool(description = "Fetch and extract text content from a web page URL")
    public String scrapeWebPage(@ToolParam(description = "The URL of the web page to scrape") String url) {
        if (StrUtil.isBlank(url)) {
            return "错误: URL 不能为空";
        }

        // Validate URL format
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "错误: URL 必须以 http:// 或 https:// 开头";
        }

        try {
            log.info("Scraping web page: {}", url);

            HttpResponse response = HttpRequest.get(url)
                    .timeout(TIMEOUT)
                    .execute();

            if (!response.isOk()) {
                return String.format("错误: HTTP 请求失败，状态码: %d", response.getStatus());
            }

            String html = response.body();
            String textContent = extractTextFromHtml(html);

            // Limit content length
            if (textContent.length() > MAX_CONTENT_LENGTH) {
                textContent = textContent.substring(0, MAX_CONTENT_LENGTH) + "...(内容已截断)";
            }

            log.info("Successfully scraped {} characters from {}", textContent.length(), url);
            return textContent;

        } catch (Exception e) {
            log.error("Failed to scrape web page: {}", url, e);
            return "错误: 抓取网页失败 - " + e.getMessage();
        }
    }

    @Tool(description = "Extract the title from a web page URL")
    public String extractTitle(@ToolParam(description = "The URL of the web page") String url) {
        if (StrUtil.isBlank(url)) {
            return "错误: URL 不能为空";
        }

        try {
            log.info("Extracting title from: {}", url);

            HttpResponse response = HttpRequest.get(url)
                    .timeout(TIMEOUT)
                    .execute();

            if (!response.isOk()) {
                return String.format("错误: HTTP 请求失败，状态码: %d", response.getStatus());
            }

            String html = response.body();
            String title = extractTitleFromHtml(html);

            if (StrUtil.isBlank(title)) {
                return "未找到标题";
            }

            log.info("Extracted title: {}", title);
            return title;

        } catch (Exception e) {
            log.error("Failed to extract title from: {}", url, e);
            return "错误: 提取标题失败 - " + e.getMessage();
        }
    }

    /**
     * Extract text content from HTML by removing tags
     */
    private String extractTextFromHtml(String html) {
        if (StrUtil.isBlank(html)) {
            return "";
        }

        // Remove script and style tags with their content
        html = html.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        html = html.replaceAll("(?i)<style[^>]*>.*?</style>", "");

        // Remove HTML comments
        html = html.replaceAll("<!--.*?-->", "");

        // Remove all HTML tags
        html = html.replaceAll("<[^>]+>", " ");

        // Decode common HTML entities
        html = html.replace("&nbsp;", " ")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&amp;", "&")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'");

        // Normalize whitespace
        html = html.replaceAll("\\s+", " ").trim();

        return html;
    }

    /**
     * Extract title from HTML
     */
    private String extractTitleFromHtml(String html) {
        if (StrUtil.isBlank(html)) {
            return "";
        }

        // Try to extract from <title> tag
        Pattern titlePattern = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = titlePattern.matcher(html);

        if (matcher.find()) {
            String title = matcher.group(1).trim();
            // Decode HTML entities
            title = title.replace("&nbsp;", " ")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'");
            return title;
        }

        return "";
    }
}
