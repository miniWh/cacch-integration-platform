package com.cacch.integration.integration.crm.client.dto;

import tools.jackson.databind.JsonNode;

/**
 * 勤策创建自定义对象数据响应
 *
 * <p>response_data 为 JSON 字符串，内容为新建数据标识，形如 {@code {"id":7985906305389659217}}，
 * 由基类二次解析后以 {@link JsonNode} 承接。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public class CrmCustomObjectSaveResponse extends CrmBaseResponse<JsonNode> {
}
