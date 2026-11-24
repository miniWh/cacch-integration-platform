package com.cacch.integration.common.constant.crm;

/**
 * 勤策 CRM 自定义对象（apaas）日志域常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmCustomObjectLogConstants {

    private CrmCustomObjectLogConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "CRM自定义对象（apaas）域业务";

    /**
     * 构造日志动作描述
     *
     * @param action 动作中文名
     * @return 原样返回的动作描述
     */
    public static String buildAction(String action) {
        return action;
    }
}
