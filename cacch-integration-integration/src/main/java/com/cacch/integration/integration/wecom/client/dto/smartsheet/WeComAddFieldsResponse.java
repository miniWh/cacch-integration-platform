package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 企微智能表格 — 添加字段响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComAddFieldsResponse extends WeComBaseResponse {

    private List<WeComFieldInfo> fields;
}
