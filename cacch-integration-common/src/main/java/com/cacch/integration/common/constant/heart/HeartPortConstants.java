package com.cacch.integration.common.constant.heart;

/**
 * Heart 客户 PO 港口解析业务参数常量
 *
 * <p>港口查询接口参数统一收敛在此，禁止在 Service 中散落字面量。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartPortConstants {

    private HeartPortConstants() {
    }

    /**
     * 港口自定义对象 key
     */
    public static final String OBJECT_KEY_PORT = "object_yEQGF__c";

    /**
     * 目的地自定义对象 key
     */
    public static final String OBJECT_KEY_DESTINATION = "object_65Zui__c";

    /**
     * 目的地名称关键字（暂固定值，后续由邮件解析结果提供）
     */
    public static final String DESTINATION_NAME_FIXED = "SHIBUSHI";

    /**
     * 港口名称字段 key
     */
    public static final String FIELD_NAME = "name";

    /**
     * 港口名称模糊匹配操作符
     */
    public static final String OPERATOR_LIKE = "LIKE";

    /**
     * 查询页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 查询每页条数
     */
    public static final int DEFAULT_ROWS = 100;

    /**
     * 响应中数据 ID 字段
     */
    public static final String RESPONSE_FIELD_ID = "id";

    /**
     * 响应中数据名称字段
     */
    public static final String RESPONSE_FIELD_NAME = "name";

    /**
     * 启运港类型标识（用于日志与异常提示）
     */
    public static final String PORT_TYPE_LOADING = "启运港";

    /**
     * 目的港类型标识（用于日志与异常提示）
     */
    public static final String PORT_TYPE_DISCHARGE = "目的港";

    /**
     * 目的地类型标识（用于日志与异常提示）
     */
    public static final String PORT_TYPE_DESTINATION = "目的地";
}
