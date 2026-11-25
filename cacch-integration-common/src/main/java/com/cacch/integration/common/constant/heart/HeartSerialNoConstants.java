package com.cacch.integration.common.constant.heart;

/**
 * Heart 客户 PO 取号业务参数常量
 *
 * <p>接口参数为固定值，统一收敛在此，禁止在 Service / Client 中散落字面量。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartSerialNoConstants {

    private HeartSerialNoConstants() {
    }

    /**
     * CRM 取号自定义对象 key
     */
    public static final String OBJECT_KEY_CAC_PO = "object_70cY4__c";

    /**
     * CAC PO 号字段 key
     */
    public static final String FIELD_CAC_PO_NO = "field_XLlRc__c";

    /**
     * 负责人字段 key
     */
    public static final String FIELD_OWNER = "owner";

    /**
     * CAC PO 号字段固定值
     */
    public static final String CAC_PO_NO_FIELD_VALUE = "4924470394950953879";

    /**
     * 固定负责人 ID
     */
    public static final String OWNER_ID = "6291185993388435139";

    /**
     * 是否触发工作流：取号不触发
     */
    public static final boolean IS_TRIGGER = false;

    /**
     * 是否校验验证规则、查重规则：取号不校验
     */
    public static final boolean CHECK_RULE = false;

    /**
     * 响应中数据 ID 字段
     */
    public static final String RESPONSE_FIELD_ID = "id";
}
