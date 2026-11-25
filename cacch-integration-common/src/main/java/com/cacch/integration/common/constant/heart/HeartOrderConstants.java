package com.cacch.integration.common.constant.heart;

/**
 * Heart 订单提交业务常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartOrderConstants {

    private HeartOrderConstants() {
    }

    /**
     * 提交状态：取号成功
     */
    public static final String SUBMIT_STATUS_SUCCESS = "SUCCESS";

    /**
     * 提交状态：无待取号 PO
     */
    public static final String SUBMIT_STATUS_NO_DATA = "NO_DATA";

    /**
     * 提交状态：本次处理失败
     */
    public static final String SUBMIT_STATUS_FAILED = "FAILED";

    /**
     * 提交记录状态：成功
     */
    public static final String RECORD_STATUS_SUCCESS = "SUCCESS";

    /**
     * 提交记录状态：失败
     */
    public static final String RECORD_STATUS_FAILED = "FAILED";
}
