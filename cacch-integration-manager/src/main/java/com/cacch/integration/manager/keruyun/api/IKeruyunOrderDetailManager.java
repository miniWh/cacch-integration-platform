package com.cacch.integration.manager.keruyun.api;

import com.cacch.integration.common.dto.keruyun.KeruyunOrderDetailSyncResult;

/**
 * 客如云订单详情同步 Manager 接口
 *
 * <p>编排链路：筛选完结订单（finish_time 非空且详情未同步）→ 取 token →
 * 固定并发拉取订单详情 → 菜品/支付明细幂等落库。单订单失败不阻断整批。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunOrderDetailManager {

    /**
     * 批量同步完结订单详情（固定并发）
     *
     * <p>仅处理主表中 finish_time 非空且 detail_synced_at 为空的订单；
     * 使用固定并发线程池拉取详情，单订单失败不阻断整批；
     * limit 用于控制单次处理量避免接口超时，剩余待同步订单可再次触发。</p>
     *
     * @param limit 单次处理订单数上限；null 时取默认 200，超过 1000 按 1000 截断
     * @return 同步统计结果（成功/失败/跳过条数与明细）
     */
    KeruyunOrderDetailSyncResult syncOrderDetails(Integer limit);

    /**
     * 同步单条订单详情（调试 / 单单补偿入口）
     *
     * <p>显式指定订单即视为重新拉取意图：已同步过的订单也会重新覆盖落库；
     * 订单不存在或未完结时终止并提示。</p>
     *
     * @param orderId 客如云订单 ID，不可为空
     * @return 同步统计结果（成功/跳过明细）
     */
    KeruyunOrderDetailSyncResult syncOrderDetail(String orderId);
}
