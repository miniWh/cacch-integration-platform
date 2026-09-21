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

    /**
     * 获取全量自定义角色接口路径 —— 相对 {@code MokaProperties#getBaseUrl()} 拼接
     *
     * <p>GET 请求，对接 Moka 开放平台角色查询接口
     * （{@linkplain <a href="https://www.mokahr.com/docs/api/?shell#-75">API 文档 #-75</a>}）。
     * 固定传 query 参数 {@code type=all} 拉取全部角色（含内建 + 自定义）。</p>
     */
    public static final String ROLE_LIST_PATH = "/api-platform/v1/users/roles";

    /**
     * Moka 用户信息同步接口路径 —— 相对 {@code MokaProperties#getBaseUrl()} 拼接
     *
     * <p>POST 请求，对接 Moka 开放平台用户同步接口
     * （{@linkplain <a href="https://www.mokahr.com/docs/api/#-72">API 文档 #-72</a>}）。</p>
     */
    public static final String USER_SYNC_INFO_PATH = "/api-platform/v1/users/syncInfo";

    // ========== Moka 用户同步 API 固定参数 ==========

    /**
     * uniqueType — Moka 用户唯一标识类型，固定传 "phone"（以手机号为唯一键）
     */
    public static final String USER_UNIQUE_TYPE = "phone";

    /**
     * autoActivated — 是否自动激活，固定传 0（不自动激活）
     */
    public static final int USER_AUTO_ACTIVATED = 0;

    /**
     * updateDepartment — 是否更新部门，固定传 false（部门不同步）
     */
    public static final boolean USER_UPDATE_DEPARTMENT = false;

    /**
     * updateSuperiorEmail — 是否更新直属领导邮箱，固定传 false（上级不同步）
     */
    public static final boolean USER_UPDATE_SUPERIOR_EMAIL = false;

    /**
     * thirdPartyId — 第三方 ID，固定传空串（SSO 未启用）
     */
    public static final String USER_THIRD_PARTY_ID = "";

    /**
     * locale — Moka 用户语言，固定传 "zh-CN"
     */
    public static final String USER_LOCALE = "zh-CN";

    /**
     * timezone — Moka 用户时区，固定传 "Asia/Shanghai"
     */
    public static final String USER_TIMEZONE = "Asia/Shanghai";

    /**
     * Moka 默认角色 ID —— 阶段一所有新同步人员固定使用此值，
     * 阶段二角色拉取后按 jobTitle 匹配 t_integration_moka_role 更新
     */
    public static final int DEFAULT_ROLE_ID = 223379;
}
