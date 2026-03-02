package com.lin.rin1aiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WebScrapingToolTest {

    @Autowired
    private WebScrapingTool webScrapingTool;

    @Test
    void testScrapeWebPage_Success() {
        // Test with a simple URL
        String result = webScrapingTool.scrapeWebPage("https://example.com");

        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("错误");
        assertThat(result.length()).isGreaterThan(0);
        assertThat(result).contains("Example Domain");

        System.out.println("Scraped content: " + result);
    }

    @Test
    void testExtractTitle_Success() {
        // Test extracting title from example.com
        String title = webScrapingTool.extractTitle("https://example.com");

        assertThat(title).isNotNull();
        assertThat(title).isNotEmpty();
        assertThat(title).contains("Example Domain");

        System.out.println("Extracted title: " + title);
    }

    @Test
    void testScrapeWebPage_WithHttpsUrl() {
        // Test with HTTPS URL
        String result = webScrapingTool.scrapeWebPage("https://www.baidu.com");

        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("错误");
        assertThat(result.length()).isGreaterThan(0);

        System.out.println("Scraped Baidu content length: " + result.length());
    }

    @Test
    void testExtractTitle_FromBaidu() {
        // Test extracting title from Baidu
        String title = webScrapingTool.extractTitle("https://www.baidu.com");

        assertThat(title).isNotNull();
        assertThat(title).isNotEmpty();
        assertThat(title).contains("百度");

        System.out.println("Extracted Baidu title: " + title);
    }

    @Test
    void testScrapeWebPage_InvalidUrlFormat() {
        // Test with invalid URL format
        String result = webScrapingTool.scrapeWebPage("invalid-url");

        assertThat(result).contains("错误");
        assertThat(result).contains("http://");
    }

    @Test
    void testScrapeWebPage_EmptyUrl() {
        // Test with empty URL
        String result = webScrapingTool.scrapeWebPage("");

        assertThat(result).contains("错误");
        assertThat(result).contains("不能为空");
    }

    @Test
    void testScrapeWebPage_NullUrl() {
        // Test with null URL
        String result = webScrapingTool.scrapeWebPage(null);

        assertThat(result).contains("错误");
        assertThat(result).contains("不能为空");
    }

    @Test
    void testExtractTitle_EmptyUrl() {
        // Test extracting title with empty URL
        String title = webScrapingTool.extractTitle("");

        assertThat(title).contains("错误");
        assertThat(title).contains("不能为空");
    }

    @Test
    void testExtractTitle_NullUrl() {
        // Test extracting title with null URL
        String title = webScrapingTool.extractTitle(null);

        assertThat(title).contains("错误");
        assertThat(title).contains("不能为空");
    }

    @Test
    void testScrapeWebPage_NonExistentDomain() {
        // Test with non-existent domain
        String result = webScrapingTool.scrapeWebPage("https://this-domain-definitely-does-not-exist-12345.com");

        assertThat(result).contains("错误");
        assertThat(result).contains("抓取网页失败");

        System.out.println("Error message: " + result);
    }

    @Test
    void testScrapeWebPage_ContentLengthLimit() {
        // Test that content is limited to MAX_CONTENT_LENGTH
        String result = webScrapingTool.scrapeWebPage("https://www.baidu.com");

        // Should not exceed 5000 characters + truncation message
        assertThat(result.length()).isLessThanOrEqualTo(5050);

        if (result.contains("内容已截断")) {
            System.out.println("Content was truncated as expected");
        }
    }

    @Test
    void testScrapeWebPage_HtmlEntityDecoding() {
        // Test that HTML entities are properly decoded
        String result = webScrapingTool.scrapeWebPage("https://example.com");

        // Should not contain HTML entities
        assertThat(result).doesNotContain("&nbsp;");
        assertThat(result).doesNotContain("&lt;");
        assertThat(result).doesNotContain("&gt;");
        assertThat(result).doesNotContain("&amp;");
    }

    @Test
    void testScrapeWebPage_NoHtmlTags() {
        // Test that HTML tags are removed
        String result = webScrapingTool.scrapeWebPage("https://example.com");

        // Should not contain HTML tags
        assertThat(result).doesNotContain("<html>");
        assertThat(result).doesNotContain("<body>");
        assertThat(result).doesNotContain("<div>");
        assertThat(result).doesNotContain("<p>");
        assertThat(result).doesNotContain("</");
    }

    @Test
    void testExtractTitle_NoTitle() {
        // Test extracting title from a page that might not have a title
        // Using a URL that returns plain text or minimal HTML
        String title = webScrapingTool.extractTitle("https://httpbin.org/robots.txt");

        // Should handle gracefully
        assertThat(title).isNotNull();
        System.out.println("Title from robots.txt: " + title);
    }
}
