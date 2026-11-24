package com.cacch.integration.integration.crm.client.dto;

import tools.jackson.databind.JsonNode;

/**
 * 勤策新增销售合同响应
 *
 * <p>response_data 为 JSON 字符串，内容由勤策定义（新增销售合同 ID），
 * 结构可能为对象或数组，故以 {@link JsonNode} 原样承接。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public class CrmContractAddResponse extends CrmBaseResponse<JsonNode> {
}
