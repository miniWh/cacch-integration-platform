package com.cacch.integration.config.fileserver;

import com.cacch.integration.common.config.fileserver.FileServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 公司内部文件服务器配置注册（REQ-OA-003 / D12）
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(FileServerProperties.class)
public class FileServerConfiguration {
}
