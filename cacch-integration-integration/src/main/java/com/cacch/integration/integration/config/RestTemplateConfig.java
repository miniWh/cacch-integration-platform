package com.cacch.integration.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * RestTemplate 配置，统一 HTTP 客户端超时与编码
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 连接超时（秒）
     */
    private static final int CONNECT_TIMEOUT = 30;

    /**
     * 读取超时（秒）
     */
    private static final int READ_TIMEOUT = 30;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT));
        factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT));
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().forEach(converter -> {
            if (converter instanceof org.springframework.http.converter.StringHttpMessageConverter stringConverter) {
                stringConverter.setDefaultCharset(StandardCharsets.UTF_8);
            }
        });
        return restTemplate;
    }
}
