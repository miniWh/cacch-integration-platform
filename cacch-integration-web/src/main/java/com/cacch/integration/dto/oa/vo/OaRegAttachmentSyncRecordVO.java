package com.cacch.integration.dto.oa.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 国内登记报告附件同步记录 VO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaRegAttachmentSyncRecordVO {

    private Long id;

    private Long formMainId;

    private String ownerName;

    private String ipdpName;

    private String itemName;

    private Long itemRowId;

    private String sharePath;

    private String fileName;

    /** @deprecated 已改用 fileCreatedAt */
    @Deprecated
    private Integer fileVersion;

    private Long fileSize;

    private String fileChecksum;

    /** 共享盘文件创建时间（幂等比对） */
    private LocalDateTime fileCreatedAt;

    private LocalDateTime fileModifiedAt;

    private String oaFileId;

    private String oaSubReference;

    private String syncStatus;

    private String syncMessage;

    private Integer retryCount;

    private LocalDateTime lastSyncAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
