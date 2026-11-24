package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 勤策创建自定义对象数据出站报文
 *
 * <p>勤策要求 fields、slave_fields 为 JSON 格式字符串，故此处以 String 承载，
 * 由 Client 使用 ObjectMapper 序列化后赋值。</p>
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
     * 主对象字段值，JSON 格式字符串
     */
    @JsonProperty("fields")
    private String fields;

    /**
     * 从对象字段值，JSON 格式字符串
     */
    @JsonProperty("slave_fields")
    private String slaveFields;

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
