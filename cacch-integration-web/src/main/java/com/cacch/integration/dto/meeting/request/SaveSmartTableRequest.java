package com.cacch.integration.dto.meeting.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 创建/更新智能表格配置请求
 * @author hongfu_zhou@cacch.com
 */
@Data
public class SaveSmartTableRequest {

    @NotBlank
    private String tableType;

    @NotBlank
    private String userId;

    @NotBlank
    private String tableName;

    @NotBlank
    private String docId;

    private String docUrl;

    @NotBlank
    private String meetingSheetId;

    @NotNull
    private Map<String, String> meetingColumnMapping;

    private String todoSheetId;

    private Map<String, String> todoColumnMapping;

    private Integer status;
}
