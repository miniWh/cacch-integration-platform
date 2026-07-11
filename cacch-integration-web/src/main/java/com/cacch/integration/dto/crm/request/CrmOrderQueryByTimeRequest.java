package com.cacch.integration.dto.crm.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按 modify_time 查询订单请求
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
     * 开始时间，格式 yyyy-MM-dd HH:mm:ss
     */
    @NotBlank(message = "beginTime 不能为空")
    private String beginTime;

    /**
     * 结束时间，格式 yyyy-MM-dd HH:mm:ss
     */
    @NotBlank(message = "endTime 不能为空")
    private String endTime;
}
