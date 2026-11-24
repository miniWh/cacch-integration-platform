package com.cacch.integration.common.constant.crm;

/**
 * 勤策 CRM 自定义对象（apaas）PATH 常量类
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmCustomObjectPathConstants {

    private CrmCustomObjectPathConstants() {
    }

    /**
     * 查询自定义对象数据
     */
    public static final String QUERY_OBJECT_DATA_PATH =
            "/api/apaas/v2/queryObjectData/{openid}/{timestamp}/{digest}/{msgId}";

    /**
     * 创建自定义对象数据
     */
    public static final String SAVE_OBJECT_DATA_PATH =
            "/api/apaas/v2/saveObjectData/{openid}/{timestamp}/{digest}/{msgId}";
}
