package com.cacch.integration.common.constant.crm;

/**
 * 勤策 CRM 商品域业务参数常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmProductConstants {

    private CrmProductConstants() {
    }

    /**
     * 查询商品默认页码
     */
    public static final int DEFAULT_PAGE_NUMBER = 1;

    /**
     * 查询商品单页条数（服务端固定每页 1000 条）
     */
    public static final int DEFAULT_PAGE_SIZE = 1000;

    /**
     * 商品分类：普通商品
     */
    public static final String CLASSIFICATION_NORMAL = "0";

    /**
     * 商品分类：助销物料
     */
    public static final String CLASSIFICATION_MATERIAL = "1";

    /**
     * 商品分类：包装物
     */
    public static final String CLASSIFICATION_PACKAGE = "2";

    /**
     * 商品分类：兑换物料
     */
    public static final String CLASSIFICATION_EXCHANGE = "3";

    /**
     * 商品状态：正常
     */
    public static final String STATUS_NORMAL = "1";

    /**
     * 商品状态：已删除
     */
    public static final String STATUS_DELETED = "0";
}
