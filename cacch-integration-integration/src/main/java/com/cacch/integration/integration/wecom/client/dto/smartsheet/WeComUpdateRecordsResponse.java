package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 企微智能表格 — 更新记录响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComUpdateRecordsResponse extends WeComBaseResponse {

    private List<WeComRecordInfo> records;
}
