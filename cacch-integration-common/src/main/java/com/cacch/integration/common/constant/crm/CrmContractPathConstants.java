package com.cacch.integration.common.constant.crm;

/**
 * 勤策 CRM 合同域 PATH 常量类
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmContractPathConstants {

    private CrmContractPathConstants() {
    }

    /**
     * 新增销售合同
     */
    public static final String CONTRACT_ADD_PATH =
            "/api/ig/v2/contractAdd/{openid}/{timestamp}/{digest}/{msgId}";
}
