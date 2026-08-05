package com.cacch.integration.dto.fdd.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 法大大认证查询/发起请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class FddAuthQueryRequest {

    /**
     * 认证类型：ENTERPRISE / PERSON
     */
    @NotBlank(message = "authType 不能为空")
    private String authType;

    /**
     * 内部企业全称（必填，业务判定键之一）
     */
    @NotBlank(message = "internalCompanyName 不能为空")
    private String internalCompanyName;

    /**
     * 外部企业名称（authType=ENTERPRISE 发起认证时必填）
     */
    private String enterpriseName;

    /**
     * 统一社会信用代码（authType=ENTERPRISE 时必填）
     */
    private String uscc;

    /**
     * 姓名：authType=PERSON 为本人；authType=ENTERPRISE 发起时为企业管理员姓名
     */
    private String personName;

    /**
     * 身份证号：authType=PERSON 为本人；authType=ENTERPRISE 发起时为企业管理员身份证号
     */
    private String idNumber;

    /**
     * 手机号：authType=PERSON 为本人；authType=ENTERPRISE 发起时为企业管理员手机号
     */
    private String mobile;

    /**
     * 是否自动发起认证，默认 true
     */
    private Boolean autoAuth = Boolean.TRUE;

    /**
     * 来源系统：CRM / OA（新发起认证时必填）
     */
    private String sourceSystem;

    /**
     * 来源系统业务单号
     */
    private String sourceBizNo;
}
