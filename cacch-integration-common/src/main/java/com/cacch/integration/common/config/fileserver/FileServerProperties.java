package com.cacch.integration.common.config.fileserver;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 公司内部文件服务器配置属性（REQ-OA-003 / D12）
 *
 * <p>OA 合同附件需上传至公司内部文件服务器换取可访问 URL 后回传 CRM。
 * 当前 FTP 上传方法未定（Q13：IP/账号/密码待提供），临时以固定 URL 占位，
 * 凭据到位后实现 FTP 上传替换。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "file-server")
public class FileServerProperties {

    /**
     * 占位固定 URL：上传方法未定期间，所有附件回传同一地址（仅验证链路通）
     */
    private final String fixedUploadUrl;

    /**
     * FTP 主机地址（凭据待提供，当前未使用）
     */
    private final String ftpHost;

    /**
     * FTP 端口（默认 21）
     */
    private final Integer ftpPort;

    /**
     * FTP 账号（凭据待提供，当前未使用）
     */
    private final String ftpUsername;

    /**
     * FTP 密码（经环境变量 FILE_SERVER_FTP_PASSWORD 注入，禁止明文与日志输出）
     */
    private final String ftpPassword;

    public FileServerProperties(String fixedUploadUrl,
                                String ftpHost,
                                Integer ftpPort,
                                String ftpUsername,
                                String ftpPassword) {
        this.fixedUploadUrl = fixedUploadUrl;
        this.ftpHost = ftpHost;
        this.ftpPort = ftpPort == null ? 21 : ftpPort;
        this.ftpUsername = ftpUsername;
        this.ftpPassword = ftpPassword;
    }
}
