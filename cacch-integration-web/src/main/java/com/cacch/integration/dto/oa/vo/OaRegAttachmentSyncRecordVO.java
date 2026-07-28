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

    private Integer fileVersion;

    private Long fileSize;

    private String fileChecksum;

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
