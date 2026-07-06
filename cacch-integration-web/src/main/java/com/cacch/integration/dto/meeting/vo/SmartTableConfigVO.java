package com.cacch.integration.dto.meeting.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 智能表格配置 VO
 * @author hongfu_zhou@cacch.com
 */
@Data
public class SmartTableConfigVO {

    private Long id;

    private String tableType;

    private String userId;

    private String tableName;

    private String docId;

    private String docUrl;

    private String meetingSheetId;

    private Map<String, String> meetingColumnMapping;

    private String todoSheetId;

    private Map<String, String> todoColumnMapping;

    private Integer status;

    private LocalDateTime lastSyncTime;

    private String lastSyncError;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
