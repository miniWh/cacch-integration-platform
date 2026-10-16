package com.cacch.integration.dto.oa.vo;

import lombok.Data;

/**
 * 法大大印章 VO（印章完整列表接口返回）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaFddSealVO {

    /**
     * 法大大公司ID
     */
    private String companyId;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 法大大印章ID
     */
    private String sealId;

    /**
     * 印章类型名称
     */
    private String sealTypeName;

    /**
     * 法大大印章管理员ID
     */
    private String adminId;

    /**
     * 印章管理员名称
     */
    private String adminName;

    /**
     * 印章管理员手机号
     */
    private String adminPhone;
}
