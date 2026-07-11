package com.cacch.integration.dto.crm.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按 modify_time 查询订单请求
 *
 * <p>CRM 侧 modify_time 过滤值为<strong>毫秒时间戳</strong>。本接口入参可传：</p>
 * <ul>
 *   <li>毫秒时间戳字符串，如 {@code 1719763200000}</li>
 *   <li>或 {@code yyyy-MM-dd HH:mm:ss}（按 Asia/Shanghai 自动转毫秒）</li>
 * </ul>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class CrmOrderQueryByTimeRequest {

    /**
     * 页码，默认 1
     */
    private Integer page;

    /**
     * 每页条数，默认 100
     */
    private Integer rows;

    /**
     * 起始修改时间（必填）：毫秒时间戳或 yyyy-MM-dd HH:mm:ss
     * <p>对应 CRM 条件：modify_time GT beginTime</p>
     */
    @NotBlank(message = "beginTime 不能为空")
    private String beginTime;

    /**
     * 结束修改时间（可选）：毫秒时间戳或 yyyy-MM-dd HH:mm:ss
     * <p>传入时追加条件：modify_time LT endTime；对齐现网「当天起」可只传 beginTime</p>
     */
    private String endTime;
}
