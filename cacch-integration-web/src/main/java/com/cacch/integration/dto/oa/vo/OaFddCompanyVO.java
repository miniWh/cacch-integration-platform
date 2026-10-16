package com.cacch.integration.dto.oa.vo;

import lombok.Data;

/**
 * 法大大公司 VO（公司去重接口返回）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaFddCompanyVO {

    /**
     * 法大大公司ID
     */
    private String companyId;

    /**
     * 公司名称
     */
    private String companyName;
}
