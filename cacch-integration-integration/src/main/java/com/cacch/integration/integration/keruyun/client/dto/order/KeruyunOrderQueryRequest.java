package com.cacch.integration.integration.keruyun.client.dto.order;

import lombok.Data;

import java.util.List;

/**
 * 客如云订单列表查询请求体
 *
 * <p>对应接口 {@code POST /open/standard/order/queryList} 的业务参数。
 * 字段名与客如云文档保持一致，避免依赖 Jackson 注解映射。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderQueryRequest {

    /**
     * 查询时间类型：FINISH_BUSI_DATE / OPEN_TIME / FINISH_TIME / SETTLE_TIME，必填
     */
    private String dateType;

    /**
     * 查询开始时间，营业日期精确到天（yyyy-MM-dd），其余精确到秒（yyyy-MM-dd HH:mm:ss），必填
     */
    private String startDate;

    /**
     * 查询结束时间，格式同 startDate，必填
     */
    private String endDate;

    /**
     * 订单类型列表，可选，取值如 FOR_HERE、PLATFORM_TAKE_OUT
     */
    private List<String> orderTypeList;

    /**
     * 订单状态列表，可选，取值如 WAIT_PROCESSED、SETTLED
     */
    private List<String> orderStatusList;

    /**
     * 分页参数
     */
    private KeruyunPageBean pageBean;
}
