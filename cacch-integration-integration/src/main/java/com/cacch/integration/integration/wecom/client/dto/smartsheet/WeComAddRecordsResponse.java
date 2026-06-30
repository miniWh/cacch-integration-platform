package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 企微智能表格 — 添加记录响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComAddRecordsResponse extends WeComBaseResponse {

    private List<WeComRecordInfo> records;
}
