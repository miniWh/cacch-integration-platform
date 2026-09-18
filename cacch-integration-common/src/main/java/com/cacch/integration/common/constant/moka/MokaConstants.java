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
     * 获取全量组织架构接口路径 —— 相对 {@code MokaProperties#getBaseUrl()} 拼接
     *
     * <p>GET 请求，返回 Moka 侧全量部门列表；支持可选 query 参数 {@code updateTimeStart}
     * （格式 yyyy-MM-dd HH:mm:ss）做增量查询，为空时返回全量。</p>
     */
    public static final String DEPT_LIST_PATH = "/api-platform/v1/departments";
}
