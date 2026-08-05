package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 法大大查询企业详情响应
 *
 * <p>兼容 {@code data} 为对象或数组：统一按列表解析，取首条。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddGetCompanyResponse {

    private Integer code;

    private String message;

    /**
     * 企业列表（兼容 object / array）
     */
    @JsonDeserialize(using = FddObjectOrArrayDeserializer.class)
    private List<FddCompanyData> data;

    private Long timestamp;

    /**
     * 取首条企业数据
     *
     * @return 首条企业；无数据时返回 null
     */
    public FddCompanyData firstCompany() {
        if (CollectionUtils.isEmpty(data)) {
            return null;
        }
        return data.getFirst();
    }

    /**
     * 是否查到企业
     *
     * @return true 表示有企业数据
     */
    public boolean hasCompany() {
        if (code == null
                || (code != FddConstants.SUCCESS_CODE && code != FddConstants.TOKEN_SUCCESS_CODE)) {
            return false;
        }
        FddCompanyData company = firstCompany();
        return company != null && StringUtils.hasText(company.getCompanyId());
    }

    /**
     * 企业是否已实名
     *
     * @return true 表示已认证
     */
    public boolean isCertified() {
        FddCompanyData company = firstCompany();
        return hasCompany()
                && company != null
                && FddConstants.ENTERPRISE_IS_CERT_CERTIFIED.equals(company.getIsCerdit());
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
