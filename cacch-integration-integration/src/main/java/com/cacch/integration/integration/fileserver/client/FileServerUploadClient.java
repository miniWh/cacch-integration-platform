package com.cacch.integration.integration.fileserver.client;

import com.cacch.integration.common.config.fileserver.FileServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 公司内部文件服务器上传客户端（REQ-OA-003 / D12）
 *
 * <p>职责：将 OA 合同附件上传至公司内部文件服务器，换取可访问 URL 后回传 CRM。</p>
 *
 * <p><strong>当前为占位实现</strong>（Q13）：FTP 上传的 IP/账号/密码待提供，上传方法未定，
 * {@link #upload(String, String)} 暂返回配置的固定 URL
 * （{@code file-server.fixed-upload-url}），仅验证「OA 附件 → 文件服务器 → CRM」链路通；
 * 凭据到位后实现 FTP 上传并替换本实现。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileServerUploadClient {

    private static final String BIZ = "文件服务器";

    private final FileServerProperties fileServerProperties;

    /**
     * 上传附件至公司内部文件服务器，换取可访问 URL
     *
     * <p>占位实现：不做真实上传，直接返回 {@code file-server.fixed-upload-url}；
     * 占位期间所有合同回传同一测试文件。</p>
     *
     * @param fileName 附件文件名（OA ctp_attachment.FILENAME），用于日志追踪
     * @param fileId   附件文件标识（OA ctp_attachment.FILE_URL），用于日志追踪
     * @return 附件访问 URL；配置缺失时返回 null
     */
    public String upload(String fileName, String fileId) {
        if (!StringUtils.hasText(fileId)) {
            log.info("【{}】上传终止, reason=附件文件标识为空, fileName={}", BIZ, fileName);
            return null;
        }
        String url = fileServerProperties.getFixedUploadUrl();
        if (!StringUtils.hasText(url)) {
            log.info("【{}】上传终止, reason=未配置占位地址(file-server.fixed-upload-url), fileName={}, fileId={}",
                    BIZ, fileName, fileId);
            return null;
        }
        log.info("【{}】上传完成(占位固定URL), fileName={}, fileId={}, url={}", BIZ, fileName, fileId, url);
        return url;
    }
}
