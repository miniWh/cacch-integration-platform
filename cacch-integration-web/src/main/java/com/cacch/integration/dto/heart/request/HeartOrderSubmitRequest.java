package com.cacch.integration.dto.heart.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Heart PO 订单提交请求
 *
 * <p>时间范围仅作用于邮件拉取入库：为空时由服务层兜底为当天
 * {@code 00:00:00} 至 {@code 23:59:59}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class HeartOrderSubmitRequest {

    /**
     * 开始时间，格式 {@code yyyy-MM-dd HH:mm:ss}，为空取当天 00:00:00
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间，格式 {@code yyyy-MM-dd HH:mm:ss}，为空取当天 23:59:59
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
