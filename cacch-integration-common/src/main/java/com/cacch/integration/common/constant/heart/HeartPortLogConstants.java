package com.cacch.integration.common.constant.heart;

/**
 * Heart 客户 PO 港口解析日志域常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartPortLogConstants {

    private HeartPortLogConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "Heart港口解析业务";

    /**
     * 构造日志动作描述
     *
     * @param action 动作名称
     * @return 动作描述
     */
    public static String buildAction(String action) {
        return action;
    }
}
