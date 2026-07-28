package com.cacch.integration.common.config.sharedrive;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 共享盘（SMB）配置
 *
 * @author hongfu_zhou@cacch.com
 */
@ConfigurationProperties(prefix = "share-drive")
public class ShareDriveProperties {

    /**
     * 协议，默认 smb
     */
    private final String protocol;

    /**
     * 根路径 UNC，如 \\192.168.1.8\国内登记资料
     */
    private final String rootPath;

    private final String username;

    private final String password;

    /**
     * 版本号正则，默认 _v(\\d+)
     */
    private final String versionPattern;

    /**
     * 允许的文件扩展名（小写，不含点）
     */
    private final List<String> allowedExtensions;

    /**
     * 单文件大小上限（字节）
     */
    private final long maxFileSizeBytes;

    public ShareDriveProperties(String protocol,
                                String rootPath,
                                String username,
                                String password,
                                String versionPattern,
                                List<String> allowedExtensions,
                                Long maxFileSizeBytes) {
        this.protocol = blankToDefault(protocol, "smb");
        this.rootPath = rootPath != null ? rootPath.trim() : "";
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password : "";
        this.versionPattern = blankToDefault(versionPattern, "_v(\\d+)");
        this.allowedExtensions = allowedExtensions != null && !allowedExtensions.isEmpty()
                ? List.copyOf(allowedExtensions)
                : List.of("pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg", "zip");
        this.maxFileSizeBytes = maxFileSizeBytes != null && maxFileSizeBytes > 0
                ? maxFileSizeBytes
                : 104_857_600L;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getVersionPattern() {
        return versionPattern;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public boolean isConfigured() {
        return !rootPath.isBlank();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
