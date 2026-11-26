package com.cacch.integration.integration.heart.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
     * 开始时间（为空时取当天 00:00:00）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间（为空时取当天 23:59:59）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
