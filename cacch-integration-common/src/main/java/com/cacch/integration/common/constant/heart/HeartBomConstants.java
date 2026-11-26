package com.cacch.integration.common.constant.heart;

import java.util.List;

/**
 * Heart BOM 版本解析常量
 *
 * <p>按明细商品编码前 9 位查询勤策 BOM 对象 {@code object_XbTYD__c}，
 * 取名称包含 {@code GA}/{@code GX} 的记录 ID 作为版本号。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartBomConstants {

    private HeartBomConstants() {
    }

    /**
     * BOM 对象 key
     */
    public static final String OBJECT_KEY_BOM = "object_XbTYD__c";

    /**
     * BOM 对象：商品编码字段 key
     */
    public static final String FIELD_PRODUCT_CODE = "field_MEK5P__c";

    /**
     * BOM 对象：名称字段 key
     */
    public static final String FIELD_BOM_NAME = "field_qbbjN__c";

    /**
     * 明细 BOM 版本字段 key
     */
    public static final String FIELD_BOM_VERSION = "field_6mBjw__c";

    /**
     * 名称固定过滤词
     */
    public static final String KEYWORD_MANUFACTURER = "哈特";

    /**
     * 名称需包含的版本关键字（忽略大小写）
     */
    public static final List<String> REQUIRED_NAME_KEYWORDS = List.of("GA", "GX");

    /**
     * 商品编码截取长度（查询用前 9 位）
     */
    public static final int PRODUCT_CODE_PREFIX_LENGTH = 9;

    /**
     * 分页页码（固定值）
     */
    public static final Integer DEFAULT_PAGE = 1;

    /**
     * 每页条数（固定值）
     */
    public static final Integer DEFAULT_ROWS = 100;

    /**
     * 查询条件组连接符
     */
    public static final String CONNECTOR_AND = "AND";
}
