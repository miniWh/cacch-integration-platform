package com.cacch.integration.dto.fdd.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 法大大认证查询/发起响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class FddAuthQueryVO {

    /**
     * 是否已认证通过
     */
    private Boolean certified;

    /**
     * 是否已发起或存在进行中认证
     */
    private Boolean needAuth;

    /**
     * 认证状态：PENDING / SUCCESS / FAILED
     */
    private String status;

    /**
     * 认证类型：ENTERPRISE / PERSON
     */
    private String authType;

    /**
     * 内部企业全称
     */
    private String internalCompanyName;

    /**
     * 法大大认证页面 URL
     */
    private String authUrl;

    /**
     * 外部企业名称
     */
    private String enterpriseName;

    /**
     * 统一社会信用代码
     */
    private String uscc;

    /**
     * 个人姓名
     */
    private String personName;

    /**
     * 身份证号
     */
    private String idNumber;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 当前命中记录发起来源（审计回显）
     */
    private String sourceSystem;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 认证通过时间
     */
    private LocalDateTime certifiedAt;

    /**
     * 是否可重试
     */
    private Boolean canRetry;

    /**
     * 提示信息
     */
    private String message;
}
