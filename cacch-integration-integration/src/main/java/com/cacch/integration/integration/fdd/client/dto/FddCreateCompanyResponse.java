package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 法大大创建企业响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddCreateCompanyResponse {

    private Integer code;

    private String message;

    private FddCreateCompanyData data;

    private Long timestamp;

    /**
     * 是否成功
     *
     * @return true 表示成功并返回 companyId
     */
    public boolean isSuccess() {
        return code != null
                && (code == FddConstants.SUCCESS_CODE || code == FddConstants.TOKEN_SUCCESS_CODE)
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
