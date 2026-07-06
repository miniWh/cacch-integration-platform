package com.cacch.integration.dto.wecom.vo;

import lombok.Data;

/**
 * 智能表格子表 VO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class SmartSheetVO {

    private String sheetId;

    private String title;

    private Boolean visible;

    /**
     * 子表类型：smartsheet / dashboard / external
     */
    private String type;
}
