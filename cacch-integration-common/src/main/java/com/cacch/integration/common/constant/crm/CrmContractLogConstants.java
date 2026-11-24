package com.cacch.integration.common.constant.crm;

/**
 * 勤策 CRM 合同域日志常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmContractLogConstants {

    private CrmContractLogConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "CRM合同域业务";

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
