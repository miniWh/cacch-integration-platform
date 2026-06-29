package com.cacch.integration.dto.wecom.vo;

import lombok.Data;

import java.util.List;

/**
 * 智能表格字段列表 VO
 *
 * @author cacch-integration
 */
@Data
public class SmartFieldListVO {

    private List<SmartFieldVO> fields;

    private Integer total;

    private Boolean hasMore;

    private Integer next;
}
