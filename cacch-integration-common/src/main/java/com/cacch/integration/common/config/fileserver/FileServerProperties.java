package com.cacch.integration.common.config.fileserver;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 公司内部文件服务器配置属性（REQ-OA-003 / D12）
 *
 * <p>OA 合同附件经 OA REST 下载后保存至 NFS 挂载目录
 * （{@code 10.80.88.38:/data/UploadFiles/oafiles} → 应用服务器 {@code /oafiles}），
 * 回传 CRM 时以「URL 前缀 + 落盘绝对路径」拼接可访问 URL。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "file-server")
public class FileServerProperties {

    /**
     * NFS 挂载根目录（落盘起始路径）
     */
    private final String mountPath;

    /**
     * 业务子目录（挂载根目录下的相对路径，按日期与 fileId 再细分）
     */
    private final String subDir;

    /**
     * CRM 回传 URL 前缀：与落盘绝对路径直接拼接（末尾斜杠自动去除）
     */
    private final String urlPrefix;

    public FileServerProperties(String mountPath, String subDir, String urlPrefix) {
        this.mountPath = normalizeDir(mountPath, "/oafiles");
        this.subDir = normalizeDir(subDir, "integration/contract-attachment");
        this.urlPrefix = urlPrefix != null && !urlPrefix.isBlank()
                ? trimTrailingSlash(urlPrefix.trim())
                : "https://crmimage.cacch.com:50002";
    }

    private static String normalizeDir(String value, String defaultValue) {
        String dir = value != null && !value.isBlank() ? value.trim() : defaultValue;
        // 统一去除首尾分隔符，避免拼接出现双斜杠
        while (dir.startsWith("/") && dir.length() > 1) {
            dir = dir.substring(1);
        }
        while (dir.endsWith("/") && dir.length() > 1) {
            dir = dir.substring(0, dir.length() - 1);
        }
        return dir;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
