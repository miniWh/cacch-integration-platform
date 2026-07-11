package com.cacch.integration.integration.crm.client.dto;

import com.cacch.integration.common.constant.crm.CrmConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 勤策 OpenAPI 统一响应
 *
 * <p>{@code response_data} 可能是对象、数组或 JSON 字符串，统一用 {@link JsonNode} 承接。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmOpenApiResponse {

    @JsonProperty("return_code")
    private String returnCode;

    @JsonProperty("return_msg")
    private String returnMsg;

    @JsonProperty("msg_id")
    private String msgId;

    @JsonProperty("response_data")
    private JsonNode responseData;

    /**
     * 是否业务成功（return_code = 0）
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return CrmConstants.RETURN_CODE_SUCCESS.equals(returnCode);
    }
}
