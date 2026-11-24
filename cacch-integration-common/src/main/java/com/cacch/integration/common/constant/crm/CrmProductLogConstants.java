package com.cacch.integration.common.constant.crm;

/**
 * 勤策 CRM 商品域日志常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmProductLogConstants {

    private CrmProductLogConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "CRM商品域业务";

    /**
     * 构建日志动作描述
     *
     * @param action 业务动作，如「查询商品」
     * @return 原样返回的动作描述
     */
    public static String buildAction(String action) {
        return action;
    }
}
