package com.lin.rin1aiagent.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 *
 * 配置 HTTP 客户端的超时时间等参数
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建 RestTemplate Bean
     *
     * 配置：
     * - 连接超时：5 秒
     * - 读取超时：10 秒
     *
     * @param builder RestTemplate 构建器
     * @return 配置好的 RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }

    /**
     * 创建 HTTP 请求工厂
     *
     * @return 配置好的请求工厂
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 秒
        factory.setReadTimeout(10000);   // 10 秒
        return factory;
    }
}
