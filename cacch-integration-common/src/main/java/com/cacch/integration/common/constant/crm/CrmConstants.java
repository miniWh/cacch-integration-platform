package com.cacch.integration.common.constant.crm;

/**
 * 勤策 CRM OpenAPI 常量
 *
 * <p>签名算法以官方文档为准：{@code digest = MD5(jsonBody + "|" + appKey + "|" + timestamp)}，
 * 并非 SHA256。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmConstants {

    private CrmConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "CrmOpenApi";

    /**
     * 订单查询路径模板：/{openid}/{timestamp}/{digest}/{msg_id}
     */
    public static final String ORDER_QUERY_PATH =
            "/api/ig/v1/orderQuery/{openid}/{timestamp}/{digest}/{msgId}";

    /**
     * 订单明细查询路径模板
     */
    public static final String ORDER_DETAIL_QUERY_PATH =
            "/api/ig/v1/orderDetailQuery/{openid}/{timestamp}/{digest}/{msgId}";

    /**
     * 员工账号查询路径模板
     */
    public static final String EMPLOYEE_QUERY_PATH =
            "/api/employee/v3/queryEmployee/{openid}/{timestamp}/{digest}/{msgId}";

    /**
     * 成功响应码
     */
    public static final String RETURN_CODE_SUCCESS = "0";

    /**
     * 默认分页页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 默认每页条数（与需求单轮上限一致）
     */
    public static final int DEFAULT_ROWS = 100;

    /**
     * 修改时间字段（采集过滤）
     */
    public static final String FIELD_MODIFY_TIME = "modify_time";

    /**
     * 订单关联字段（明细按订单过滤）
     */
    public static final String FIELD_ORDER_ID = "order_id";

    /**
     * 创建时间字段（默认排序）
     */
    public static final String FIELD_CREATE_TIME = "create_time";
}
