package com.cacch.integration.integration.config;

import com.cacch.integration.common.config.oa.OaProperties;
import org.springframework.beans.factory.annotation.Qualifier;
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
     * 默认读取超时（秒）
     */
    private static final int READ_TIMEOUT = 30;

    @Bean
    public RestTemplate restTemplate() {
        return buildRestTemplate(CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * OA 大文件附件上传专用 RestTemplate（长读超时）
     *
     * @param oaProperties OA 配置
     * @return 上传专用 RestTemplate
     */
    @Bean
    @Qualifier("oaUploadRestTemplate")
    public RestTemplate oaUploadRestTemplate(OaProperties oaProperties) {
        int readTimeout = oaProperties.getAttachmentUploadReadTimeoutSeconds() > 0
                ? oaProperties.getAttachmentUploadReadTimeoutSeconds()
                : 1800;
        return buildRestTemplate(CONNECT_TIMEOUT, readTimeout);
    }

    private static RestTemplate buildRestTemplate(int connectTimeoutSeconds, int readTimeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().forEach(converter -> {
            if (converter instanceof org.springframework.http.converter.StringHttpMessageConverter stringConverter) {
                stringConverter.setDefaultCharset(StandardCharsets.UTF_8);
            }
        });
        return restTemplate;
    }
}
