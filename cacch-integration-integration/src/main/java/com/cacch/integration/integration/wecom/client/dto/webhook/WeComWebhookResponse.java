package com.cacch.integration.integration.wecom.client.dto.webhook;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企微 Webhook 通用响应
 *
 * <p>全部字段（errCode / errMsg / isSuccess）继承自 {@link WeComBaseResponse}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComWebhookResponse extends WeComBaseResponse {
}
