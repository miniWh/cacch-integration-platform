package com.cacch.integration.integration.oa.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 致远 OA 人员信息（orgMembers 常用字段）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OaOrgMember {

    /**
     * OA 人员 ID（映射业务员 / 发起人）
     */
    private String id;

    /**
     * 人员编号（对应 CRM emp_code）
     */
    private String code;

    /**
     * 姓名
     */
    private String name;

    /**
     * 登录名（Token loginName / 发起人）
     */
    @JsonProperty("loginName")
    private String loginName;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
