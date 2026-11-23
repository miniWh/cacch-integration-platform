package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * iHR 修改员工基础信息 — 请求体（PUT /openapi/thirdparty/api/v1/staff/{id}）
 *
 * <p>仅传需更新字段；不传不处理，传空会清空。花名册自定义项通过 {@link #setCustomField} 写入 JSON 根节点。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IhrStaffUpdateBasicInfoRequest {

    /**
     * 员工姓名，最大 32 位
     */
    private String staffName;

    /**
     * 员工本人手机号；修改会触发与企业微信等平台解绑，非必要勿传
     */
    private String mobileNo;

    /**
     * 证件号，最大 30 位（敏感）
     */
    private String idCardNo;

    /**
     * 证件类型，如 {@code IDENTITY_CARD}，参见 iHR 文档枚举
     */
    private String idCardType;

    /**
     * 性别：{@code MALE} / {@code FEMALE}
     */
    private String sex;

    /**
     * 员工类型，如 {@code FULLTIME}，参见 {@link com.cacch.integration.common.constant.ihr.IhrStaffConstants}
     */
    private String staffType;

    /**
     * 合同类型，如 {@code LABOR_CONTRACT}
     */
    private String contractType;

    /**
     * 个人邮箱，最大 128 位
     */
    private String email;

    /**
     * 紧急联系人姓名，最大 30 位
     */
    private String emergencyContactName;

    /**
     * 紧急联系人手机号码，最大 18 位（敏感，日志须脱敏）
     */
    private String emergencyContactMobile;

    @JsonIgnore
    private final Map<String, Object> customFields = new LinkedHashMap<>();

    /**
     * 设置花名册自定义字段（序列化到请求 JSON 根节点，key 来自「查询花名册字段信息」）
     *
     * @param fieldKey   自定义字段 key
     * @param fieldValue 自定义字段值（如紧急联系人关系下拉值）
     */
    public void setCustomField(String fieldKey, Object fieldValue) {
        if (fieldKey != null && !fieldKey.isBlank()) {
            customFields.put(fieldKey, fieldValue);
        }
    }

    @JsonAnyGetter
    public Map<String, Object> anyCustomFields() {
        return customFields.isEmpty() ? null : customFields;
    }

    @JsonAnySetter
    public void anyCustomField(String key, Object value) {
        customFields.put(key, value);
    }
}