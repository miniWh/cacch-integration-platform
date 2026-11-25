package com.cacch.integration.common.constant.wecom;

/**
 * 企业微信邮件日志常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class WeComMailLogConstants {

    private WeComMailLogConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "企业微信邮件业务";

    /**
     * 构建日志操作名称
     *
     * @param action 接口中文名称
     * @return 日志操作名称
     */
    public static String buildAction(String action) {
        return action;
    }
}
