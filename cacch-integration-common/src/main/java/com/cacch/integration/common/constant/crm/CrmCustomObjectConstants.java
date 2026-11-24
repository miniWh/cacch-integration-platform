package com.cacch.integration.common.constant.crm;

/**
 * 勤策 CRM 自定义对象（apaas）业务参数常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmCustomObjectConstants {

    private CrmCustomObjectConstants() {
    }

    /**
     * 查询自定义对象数据默认页码
     */
    public static final int DEFAULT_PAGE_NUMBER = 1;

    /**
     * 查询自定义对象数据默认单页条数
     */
    public static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * 默认排序字段（文档约定默认按创建时间倒序）
     */
    public static final String DEFAULT_SORT_FIELD = "create_time";

    /**
     * 排序类型：正序
     */
    public static final String SORT_TYPE_ASC = "asc";

    /**
     * 排序类型：倒序
     */
    public static final String SORT_TYPE_DESC = "desc";

    /**
     * 条件组连接符：且
     */
    public static final String CONNECTOR_AND = "AND";

    /**
     * 条件组连接符：或
     */
    public static final String CONNECTOR_OR = "OR";
}
