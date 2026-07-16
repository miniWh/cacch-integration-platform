package com.cacch.integration.common.constant.crm;

/**
 * CRM→OA 表单固定值与业务常量（对齐 REQ-CRM-001 / ESB 默认值）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmOaFormConstants {

    private CrmOaFormConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "CrmOaSync";

    /**
     * 销售组织 field0007
     */
    public static final String SALES_ORG = "CAC02";

    /**
     * 内外贸 field0019（ESB 占位默认值，待业务确认后更新）
     */
    public static final String TRADE_TYPE = "内外贸待确认字段";

    /**
     * 是否集中采购 field0020
     */
    public static final String CENTRAL_PURCHASE = "1";

    /**
     * 委托放行单 field0031
     */
    public static final String ENTRUST_RELEASE = "1";

    /**
     * 委外方式 field0035
     */
    public static final String OUTSOURCE_MODE = "1";

    /**
     * 客户主键 field0327
     */
    public static final String CUSTOMER_PK = "S1201280054";

    /**
     * 原币信用额度 field0408
     */
    public static final String CREDIT_LIMIT = "空值";

    /**
     * 发货日期 field0129（本期空串）
     */
    public static final String SHIP_DATE_EMPTY = "";

    /**
     * 是否需要盖章：是
     */
    public static final String SEAL_YES = "1";

    /**
     * 是否需要盖章：否
     */
    public static final String SEAL_NO = "2";

    /**
     * CRM 盖章 code=是
     */
    public static final String CRM_SEAL_CODE_YES = "1";

    /**
     * 零售包装：是（pd_code 以 53 开头）
     */
    public static final String RETAIL_YES = "0001";

    /**
     * 零售包装：否
     */
    public static final String RETAIL_NO = "0002";

    /**
     * 零售包装判断前缀
     */
    public static final String RETAIL_PD_CODE_PREFIX = "53";

    /**
     * OA 同步默认最大重试次数
     */
    public static final int DEFAULT_MAX_RETRY = 3;
}
