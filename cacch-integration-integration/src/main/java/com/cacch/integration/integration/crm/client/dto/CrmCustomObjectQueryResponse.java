package com.cacch.integration.integration.crm.client.dto;

import java.util.List;
import java.util.Map;

/**
 * 勤策查询自定义对象数据响应
 *
 * <p>自定义对象字段由业务方动态配置（{@code field_xxx__c}），且值可能是标量、
 * {@code {format,value}}、{@code {code,label}} 或数组，故 response_data 使用弱类型
 * {@code List<Map<String,Object>>} 承接，不做强类型映射。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public class CrmCustomObjectQueryResponse extends CrmBaseResponse<List<Map<String, Object>>> {
}
