package com.cacch.integration.integration.heart.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件拉取请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class HeartMailQueryRequest {

    /**
     * 拉取起始时间（为空时取当天 00:00）
     */
    private LocalDateTime since;
}
