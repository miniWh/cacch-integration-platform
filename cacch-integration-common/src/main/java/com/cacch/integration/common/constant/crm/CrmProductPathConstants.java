package com.cacch.integration.common.constant.crm;

/**
 * 勤策 CRM 商品域开放平台 PATH 常量类
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmProductPathConstants {

    private CrmProductPathConstants() {
    }

    /**
     * 查询商品
     */
    public static final String QUERY_PRODUCT_PATH = "/api/product/v1/queryProduct/{openid}/{timestamp}/{digest}/{msgId}";
}
