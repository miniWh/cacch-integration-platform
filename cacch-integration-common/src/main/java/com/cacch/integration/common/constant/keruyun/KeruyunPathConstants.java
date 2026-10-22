package com.cacch.integration.common.constant.keruyun;

/**
 * 客如云开放平台接口路径常量
 *
 * <p>仅承载各接口 PATH，请求参数见 {@link KeruyunCommonConstants} 与 {@link KeruyunOrderConstants}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class KeruyunPathConstants {

    private KeruyunPathConstants() {
    }

    /**
     * 获取门店 token 接口路径（GET）
     */
    public static final String PATH_GET_TOKEN = "/open/v1/token/get";

    /**
     * 订单列表查询接口路径（POST）
     */
    public static final String PATH_QUERY_ORDER_LIST = "/open/standard/order/queryList";
}
