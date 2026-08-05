package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 法大大查询企业详情响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddGetCompanyResponse {

    private Integer code;

    private String message;

    private FddCompanyData data;

    private Long timestamp;

    /**
     * 是否查到企业
     *
     * @return true 表示有企业数据
     */
    public boolean hasCompany() {
        return code != null
                && (code == FddConstants.SUCCESS_CODE || code == FddConstants.TOKEN_SUCCESS_CODE)
                && data != null
                && data.getCompanyId() != null
                && !data.getCompanyId().isBlank();
    }

    /**
     * 企业是否已实名
     *
     * @return true 表示已认证
     */
    public boolean isCertified() {
        return hasCompany()
                && FddConstants.ENTERPRISE_IS_CERT_CERTIFIED.equals(data.getIsCerdit());
    }

    /**
     * 企业详情
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FddCompanyData {

        @JsonProperty("companyId")
        private String companyId;

        @JsonProperty("tpOrgId")
        private String tpOrgId;

        @JsonProperty("companyName")
        private String companyName;

        /**
         * 认证状态：1未认证,2认证中,3已认证,4认证失败,5认证失效,6待授权,7授权失败（文档字段名为 isCerdit）
         */
        @JsonProperty("isCerdit")
        private String isCerdit;
    }
}
