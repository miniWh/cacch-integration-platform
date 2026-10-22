package com.cacch.integration.common.constant.keruyun;

/**
 * 客如云订单接口专有参数常量
 *
 * <p>仅承载订单列表查询接口（{@link KeruyunPathConstants#PATH_QUERY_ORDER_LIST}）的
 * 时间类型、时间格式、查询跨度与分页限制。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class KeruyunOrderConstants {

    private KeruyunOrderConstants() {
    }

    /**
     * 查询时间类型：营业日期（日期格式 yyyy-MM-dd）
     */
    public static final String DATE_TYPE_FINISH_BUSI_DATE = "FINISH_BUSI_DATE";

    /**
     * 营业日期查询的时间格式
     */
    public static final String DATE_PATTERN_DAY = "yyyy-MM-dd";

    /**
     * 其余时间类型查询的时间格式
     */
    public static final String DATE_PATTERN_SECOND = "yyyy-MM-dd HH:mm:ss";

    /**
     * 订单查询允许的最大时间跨度（天）
     */
    public static final int QUERY_MAX_DAYS = 31;

    /**
     * 订单查询默认每页条数
     */
    public static final int ORDER_PAGE_SIZE_DEFAULT = 50;

    /**
     * 订单查询最大每页条数
     */
    public static final int ORDER_PAGE_SIZE_MAX = 100;

    /**
     * 订单查询最大翻页保护值，防止接口异常返回 totalPage 导致死循环
     */
    public static final int ORDER_MAX_PAGE_GUARD = 200;
}
