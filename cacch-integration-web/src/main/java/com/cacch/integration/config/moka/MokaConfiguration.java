package com.cacch.integration.config.moka;

import com.cacch.integration.common.config.moka.MokaProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Moka 开放平台配置注册 —— 将 {@link MokaProperties} 注册为 Spring Bean
 *
 * <p>启动时打印一次配置诊断日志（仅打印长度，严禁打印密钥值），
 * 便于快速确认环境变量 {@code MOKA_API_KEY} 是否已正确注入。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MokaProperties.class)
@RequiredArgsConstructor
public class MokaConfiguration {

    private final MokaProperties mokaProperties;

    @PostConstruct
    public void logConfigDiagnosis() {
        String apiKey = mokaProperties.getApiKey();
        int keyLen = apiKey == null ? 0 : apiKey.length();
        String baseUrl = mokaProperties.getBaseUrl();
        boolean configured = mokaProperties.isApiKeyConfigured();

        if (configured) {
            log.info("【Moka】配置加载完成, baseUrl={}, apiKey.len={}", baseUrl, keyLen);
        } else {
            log.warn("【Moka】API Key 未配置！请在启动脚本 export MOKA_API_KEY=xxx 后重启服务。baseUrl={}, apiKey.len=0", baseUrl);
        }
    }
}
