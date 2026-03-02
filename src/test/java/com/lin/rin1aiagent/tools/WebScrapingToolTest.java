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
    void testScrapeWebPage() {
        // Test with a simple URL
        String result = webScrapingTool.scrapeWebPage("https://example.com");

        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("错误");
        assertThat(result.length()).isGreaterThan(0);

        System.out.println("Scraped content: " + result);
    }

    @Test
    void testExtractTitle() {
        // Test extracting title
        String title = webScrapingTool.extractTitle("https://example.com");

        assertThat(title).isNotNull();
        assertThat(title).isNotEmpty();

        System.out.println("Extracted title: " + title);
    }

    @Test
    void testScrapeWithInvalidUrl() {
        // Test with invalid URL
        String result = webScrapingTool.scrapeWebPage("invalid-url");

        assertThat(result).contains("错误");
    }

    @Test
    void testScrapeWithEmptyUrl() {
        // Test with empty URL
        String result = webScrapingTool.scrapeWebPage("");

        assertThat(result).contains("错误");
        assertThat(result).contains("不能为空");
    }
}
