package com.cacch.integration.dto.keruyun.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客如云订单手动跨天同步请求
 *
 * <p>dateType 固定为 OPEN_TIME，由后端强制指定，调用方无需传入；
 * 调用方仅需指定查询窗口起始与结束时间。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderManualSyncRequest {

    /**
     * 查询窗口起始时间
     */
    @NotNull(message = "查询窗口起始时间不可为空")
    private LocalDateTime startTime;

    /**
     * 查询窗口结束时间，必须晚于 startTime
     */
    @NotNull(message = "查询窗口结束时间不可为空")
    private LocalDateTime endTime;
}
