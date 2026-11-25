package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 勤策创建自定义对象数据出站报文
 *
 * <p>fields、slave_fields 以 JSON 对象结构出站（不可序列化为字符串），
 * 由 Jackson 直接序列化，签名与请求体使用同一份 JSON。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmCustomObjectSavePayload {

    /**
     * 自定义对象 key
     */
    @JsonProperty("object_key")
    private String objectKey;

    /**
     * 主对象字段值，格式 {字段key: 字段值}
     */
    @JsonProperty("fields")
    private Map<String, Object> fields;

    /**
     * 从对象字段值，格式 {从对象key: [{字段key: 字段值}]}
     */
    @JsonProperty("slave_fields")
    private Map<String, List<Map<String, Object>>> slaveFields;

    /**
     * 是否触发工作流
     */
    @JsonProperty("is_trigger")
    private Boolean isTrigger;

    /**
     * 触发员工 ID
     */
    @JsonProperty("trigger_employee")
    private String triggerEmployee;

    /**
     * 是否校验验证规则、查重规则
     */
    @JsonProperty("check_rule")
    private Boolean checkRule;
}
