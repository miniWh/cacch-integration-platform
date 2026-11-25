package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 勤策创建自定义对象数据请求参数（入站）
 *
 * <p>测试端点与上层调用使用驼峰命名；出站由 {@link CrmCustomObjectSavePayload} 转为 snake_case，
 * 其中 fields、slave_fields 保持 JSON 对象结构出站。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmCustomObjectSaveRequest {

    /**
     * 自定义对象 key，必填
     */
    @JsonProperty("object_key")
    @JsonAlias("objectKey")
    private String objectKey;

    /**
     * 主对象字段值，格式 {字段key: 字段值}
     */
    private Map<String, Object> fields;

    /**
     * 从对象字段值，格式 {从对象key: [{字段key: 字段值}]}；带 id 为编辑，无 id 为新增
     */
    @JsonProperty("slave_fields")
    @JsonAlias("slaveFields")
    private Map<String, List<Map<String, Object>>> slaveFields;

    /**
     * 是否触发工作流
     */
    @JsonProperty("is_trigger")
    @JsonAlias("isTrigger")
    private Boolean isTrigger;

    /**
     * 触发员工 ID；is_trigger=true 时必填
     */
    @JsonProperty("trigger_employee")
    @JsonAlias("triggerEmployee")
    private String triggerEmployee;

    /**
     * 是否校验验证规则、查重规则
     */
    @JsonProperty("check_rule")
    @JsonAlias("checkRule")
    private Boolean checkRule;
}
