package com.cacch.integration.common.constant.heart;

/**
 * Heart BOM 版本解析日志域常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartBomLogConstants {

    private HeartBomLogConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "HeartBOM版本解析业务";

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
