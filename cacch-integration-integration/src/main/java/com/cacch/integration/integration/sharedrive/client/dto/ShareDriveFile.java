package com.cacch.integration.integration.sharedrive.client.dto;

import java.time.LocalDateTime;

/**
 * 共享盘最新版文件描述
 *
 * @author hongfu_zhou@cacch.com
 */
public record ShareDriveFile(
        String fileName,
        int fileVersion,
        long fileSize,
        String checksum,
        LocalDateTime modifiedAt,
        byte[] content,
        String contentType
) {
}
