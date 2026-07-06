package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企微智能表格 — 添加子表响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComAddSheetResponse extends WeComBaseResponse {

    private WeComSheetProperties properties;
}
