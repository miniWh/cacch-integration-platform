package com.cacch.integration.integration.wecom.client.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企微 Webhook 通用响应
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComWebhookResponse {

    @JsonProperty("errcode")
    private int errCode;

    @JsonProperty("errmsg")
    private String errMsg;

    public boolean isSuccess() {
        return errCode == 0;
    }
}
