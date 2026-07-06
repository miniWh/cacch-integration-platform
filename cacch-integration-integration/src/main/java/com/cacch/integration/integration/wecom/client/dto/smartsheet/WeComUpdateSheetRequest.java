package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import lombok.Builder;
import lombok.Data;

/**
 * 企微智能表格 — 更新子表请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComUpdateSheetRequest {

    private String docid;

    private WeComSheetProperties properties;
}
