package com.cacch.integration.dto.wecom.vo;

import lombok.Data;

import java.util.List;

/**
 * 智能表格记录列表 VO
 *
 * @author cacch-integration
 */
@Data
public class SmartRecordListVO {

    private List<SmartRecordVO> records;

    private Integer total;

    private Boolean hasMore;

    private Integer next;

    private Integer ver;
}
