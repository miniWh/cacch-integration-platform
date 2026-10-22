package com.cacch.integration.dto.keruyun.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 客如云订单同步请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderSyncRequest {

    /**
     * 查询时间类型：FINISH_BUSI_DATE（营业日期）/ OPEN_TIME（下单时间）/
     * FINISH_TIME（完结时间）/ SETTLE_TIME（结账时间）
     */
    @NotBlank(message = "dateType 不能为空")
    private String dateType;

    /**
     * 查询开始时间：FINISH_BUSI_DATE 用 yyyy-MM-dd，其余用 yyyy-MM-dd HH:mm:ss
     */
    @NotBlank(message = "startDate 不能为空")
    private String startDate;

    /**
     * 查询结束时间，格式同 startDate；与 startDate 跨度不得超过 31 天
     */
    @NotBlank(message = "endDate 不能为空")
    private String endDate;

    /**
     * 订单类型过滤列表，可选，如 FOR_HERE、PLATFORM_TAKE_OUT
     */
    private List<String> orderTypeList;

    /**
     * 订单状态过滤列表，可选，如 WAIT_PROCESSED、SETTLED
     */
    private List<String> orderStatusList;

    /**
     * 每页条数，可选，默认 50，最大 100
     */
    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 100, message = "pageSize 最大为 100")
    private Integer pageSize;
}
