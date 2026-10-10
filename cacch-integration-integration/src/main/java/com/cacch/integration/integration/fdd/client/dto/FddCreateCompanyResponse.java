package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 法大大创建企业响应
 *
 * <p>通用字段（code / message / timestamp）继承自 {@link FddBaseResponse}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddCreateCompanyResponse extends FddBaseResponse {

    private FddCreateCompanyData data;

    /**
     * 是否成功
     *
     * @return true 表示成功并返回 companyId
     */
    public boolean isSuccess() {
        Integer c = getCode();
        return c != null
                && (c == FddConstants.SUCCESS_CODE || c == FddConstants.TOKEN_SUCCESS_CODE)
                && data != null
                && data.getCompanyId() != null
                && !data.getCompanyId().isBlank();
    }

    /**
     * 创建企业数据
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FddCreateCompanyData {

        @JsonProperty("companyId")
        private String companyId;

        @JsonProperty("accountId")
        private String accountId;
    }
}
