package com.cacch.integration.dto.wecom.vo;

import lombok.Data;

import java.util.Map;

/**
 * 智能表格记录 VO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class SmartRecordVO {

    private String recordId;

    private String createTime;

    private String updateTime;

    /**
     * 单元格内容，key 为字段 ID
     */
    private Map<String, Object> values;

    private String creatorName;

    private String updaterName;
}
