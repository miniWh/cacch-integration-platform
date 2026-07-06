package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企微 API 通用响应基类
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComBaseResponse {

    @JsonProperty("errcode")
    private int errCode;

    @JsonProperty("errmsg")
    private String errMsg;

    public boolean isSuccess() {
        return errCode == 0;
    }
}
