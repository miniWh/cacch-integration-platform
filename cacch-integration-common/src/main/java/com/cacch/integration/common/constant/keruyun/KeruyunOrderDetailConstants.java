package com.cacch.integration.common.constant.keruyun;

/**
 * 客如云订单详情接口专有参数常量
 *
 * <p>仅承载订单详情查询接口（{@link KeruyunPathConstants#PATH_QUERY_ORDER_DETAIL}）的
 * 并发与批量控制参数。接口限制最大 QPS 220/秒，采用固定并发数控制调用速率。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class KeruyunOrderDetailConstants {

    private KeruyunOrderDetailConstants() {
    }

    /**
     * 详情同步固定并发数（同时发起详情查询的线程数）
     *
     * <p>接口限制最大 QPS 220/秒，固定 5 并发下单请求峰值远低于限流阈值。</p>
     */
    public static final int DETAIL_SYNC_CONCURRENCY = 5;

    /**
     * 单次批量详情同步默认处理订单数上限
     */
    public static final int DETAIL_SYNC_BATCH_DEFAULT = 200;

    /**
     * 单次批量详情同步允许的订单数上限（超过按此值截断，调用方可分多次触发）
     */
    public static final int DETAIL_SYNC_BATCH_MAX = 1000;
}
