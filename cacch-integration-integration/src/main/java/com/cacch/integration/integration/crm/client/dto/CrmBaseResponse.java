package com.cacch.integration.integration.crm.client.dto;

import com.cacch.integration.common.constant.crm.CrmConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 勤策 OpenAPI 统一响应基类
 *
 * <p>承载 {@code return_code / return_msg / msg_id / response_data} 统一信封字段与成功判断；
 * 各接口响应 DTO 继承本类并指定 {@code response_data} 的具体类型，避免重复声明。</p>
 *
 * @param <T> response_data 的具体类型
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CrmBaseResponse<T> {

    /**
     * 返回码，0 表示成功
     */
    @JsonProperty("return_code")
    private String returnCode;

    /**
     * 返回消息
     */
    @JsonProperty("return_msg")
    private String returnMsg;

    /**
     * 消息 ID
     */
    @JsonProperty("msg_id")
    private String msgId;

    /**
     * 业务数据，对应 response_data
     */
    @JsonProperty("response_data")
    private T responseData;

    /**
     * 是否业务成功（return_code = 0）
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return CrmConstants.RETURN_CODE_SUCCESS.equals(returnCode);
    }
}
