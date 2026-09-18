package com.cacch.integration.common.constant.moka;

/**
 * Moka 开放平台相关常量
 *
 * <p>仅承载第三方对接的 URL 路径与成功判定常量。
 * 网关地址由 yml 的 {@code moka.base-url} 提供，业务 URL 在运行时拼接；
 * 成功判定同时兼容 Moka 文档示例的 {@code code=0} 与返回字段表的 {@code code=200} 两种口径。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MokaConstants {

    private MokaConstants() {
    }

    /**
     * 日志业务标识 —— integration Client / Service / Controller 日志均以此为前缀
     */
    public static final String LOG_BIZ = "Moka";

    /**
     * 组织架构全量同步接口路径 —— 相对 {@code MokaProperties#getBaseUrl()} 拼接
     */
    public static final String DEPT_FULL_SYNC_PATH = "/api-platform/v2/departments";

    /**
     * 组织架构增量同步接口路径 —— 相对 {@code MokaProperties#getBaseUrl()} 拼接
     */
    public static final String DEPT_INCREMENTAL_SYNC_PATH = "/api-platform/v2/departments/sync/incremental";

    /**
     * 成功业务码（文档示例口径，Moka 实际响应 code=0 表示成功）
     */
    public static final int RESPONSE_CODE_SUCCESS = 0;

    /**
     * 成功业务码（文档返回字段表口径，返回字段表格中写 200 为成功）
     */
    public static final int RESPONSE_CODE_SUCCESS_LEGACY = 200;

    /**
     * 判断 Moka 响应 code 是否为成功 —— 兼容两种口径
     *
     * @param code Moka 响应体中的 code 字段
     * @return true=成功；false=失败
     */
    public static boolean isSuccessCode(int code) {
        return code == RESPONSE_CODE_SUCCESS || code == RESPONSE_CODE_SUCCESS_LEGACY;
    }
}
