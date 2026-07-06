package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微智能表格 — 添加子表请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComAddSheetRequest {

    private String docid;

    private WeComSheetProperties properties;
}
