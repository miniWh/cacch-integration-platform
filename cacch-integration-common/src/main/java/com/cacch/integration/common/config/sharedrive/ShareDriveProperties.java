package com.cacch.integration.common.config.sharedrive;

import com.cacch.integration.common.constant.sharedrive.ShareDriveConstants;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 共享盘（SMB）配置
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
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
     * Windows 域名称；留空表示工作组或服务器本地账号
     */
    private final String domain;

    /**
     * 最终版本文件名后缀（主文件名不含扩展名须以此结尾），默认 {@code _最终版本}
     */
    private final String finalVersionSuffix;

    /**
     * @deprecated 已改用 {@link #finalVersionSuffix} 筛选最终版本文件，保留配置项仅为兼容
     */
    @Deprecated
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
                                String domain,
                                String finalVersionSuffix,
                                String versionPattern,
                                List<String> allowedExtensions,
                                Long maxFileSizeBytes) {
        this.protocol = blankToDefault(protocol, "smb");
        this.rootPath = rootPath != null ? rootPath.trim() : "";
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password : "";
        this.domain = domain != null ? domain.trim() : "";
        this.finalVersionSuffix = blankToDefault(finalVersionSuffix, ShareDriveConstants.DEFAULT_FINAL_VERSION_SUFFIX);
        this.versionPattern = blankToDefault(versionPattern, "_v(\\d+)");
        this.allowedExtensions = allowedExtensions != null && !allowedExtensions.isEmpty()
                ? List.copyOf(allowedExtensions)
                : List.of("pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg", "zip");
        this.maxFileSizeBytes = maxFileSizeBytes != null && maxFileSizeBytes > 0
                ? maxFileSizeBytes
                : 104_857_600L;
    }

    public boolean isConfigured() {
        return !rootPath.isBlank();
    }

    /**
     * 是否已配置 SMB 登录账号（UNC 远程访问必填，Guest 在多数服务器已禁用）
     *
     * @return true 表示已配置 username
     */
    public boolean hasCredentials() {
        return !username.isBlank();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
