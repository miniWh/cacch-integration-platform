package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 企微智能表格记录信息
 *
 * @author cacch-integration
 */
@Data
public class WeComRecordInfo {

    @JsonProperty("record_id")
    private String recordId;

    @JsonProperty("create_time")
    private String createTime;

    @JsonProperty("update_time")
    private String updateTime;

    /**
     * 单元格内容，key 为字段标题或字段 ID
     */
    private Map<String, Object> values;

    @JsonProperty("creator_name")
    private String creatorName;

    @JsonProperty("updater_name")
    private String updaterName;
}
