package com.cacch.integration.manager.keruyun.api;

import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncResult;

import java.util.List;

/**
 * 客如云订单同步编排 Manager
 *
 * <p>负责「时间窗校验 → 逐页调用客如云订单列表 → 单条落库」的编排，
 * HTTP 调用与数据库事务分别由 Client、Service 承担。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunOrderManager {

    /**
     * 按时间窗同步客如云订单到本地 stg_keruyun_order
     *
     * <p>查询跨度不得超过 31 天；单条落库失败不阻断整批，最终返回统计结果。</p>
     *
     * @param dateType        查询时间类型：FINISH_BUSI_DATE / OPEN_TIME / FINISH_TIME / SETTLE_TIME，不可为空
     * @param startDate       查询开始时间，FINISH_BUSI_DATE 用 yyyy-MM-dd，其余用 yyyy-MM-dd HH:mm:ss
     * @param endDate         查询结束时间，格式同 startDate
     * @param orderTypeList   订单类型过滤列表，可为 null（不限制）
     * @param orderStatusList 订单状态过滤列表，可为 null（不限制）
     * @param pageSize        每页条数，为 null 时默认 50，超过 100 时按 100 截断
     * @return 同步统计结果
     * @throws com.cacch.integration.common.exception.BizException 时间类型非法、时间格式错误、
     *                                                             区间超过 31 天或客如云接口调用失败时抛出
     */
    KeruyunOrderSyncResult syncOrders(String dateType, String startDate, String endDate,
                                      List<String> orderTypeList, List<String> orderStatusList,
                                      Integer pageSize);
}
