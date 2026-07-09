package com.cacch.integration.integration.wecom.client.dto.user;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企微通讯录 — 读取成员详情响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeComGetUserResponse extends WeComBaseResponse {

    /**
     * 成员 UserID
     */
    private String userid;

    /**
     * 成员名称
     */
    private String name;

    /**
     * 别名
     */
    private String alias;

    /**
     * 手机号（需通讯录权限，可能为空）
     */
    private String mobile;

    /**
     * 职务信息
     */
    private String position;

    /**
     * 激活状态：1=已激活，2=已禁用，4=未激活，5=退出企业
     */
    private Integer status;

    /**
     * 主部门 ID
     */
    @JsonProperty("main_department")
    private Long mainDepartment;
}
