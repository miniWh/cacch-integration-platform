package com.cacch.integration.integration.sharedrive.client.dto;

import java.time.LocalDateTime;

/**
 * 共享盘资料项目目录内待同步文件元数据（不含文件内容，内容通过流式读取）
 *
 * @param fileName    文件名
 * @param fileSize    大小（字节）
 * @param createdAt   文件创建时间（幂等比对主键）
 * @param modifiedAt  最后修改时间
 * @param contentType MIME 类型
 * @author hongfu_zhou@cacch.com
 */
public record ShareDriveFile(
        String fileName,
        long fileSize,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        String contentType
) {
}
