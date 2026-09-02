package com.cacch.integration.common.constant.ihr;

import com.cacch.integration.common.constant.redis.RedisConstants;

/**
 * IHR 开放平台相关常量
 *
 * <p>仅承载第三方对接的 URL、TTL、搜索类型枚举与 Redis Key 模板。
 * 业务规则由 Service/Manager 层负责。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class IhrConstants {

    private IhrConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "iHR";

    /**
     * IHR 开放平台网关域名
     */
    public static final String BASE_URL = "https://openapi.cacch.com:776";

    /**
     * OAuth2 Token 获取 / 刷新地址。
     * <p>通过 {@code grant_type} query 参数区分：
     * <ul>
     *     <li>{@code client_credentials} — 首次或 refresh_token 过期后获取</li>
     *     <li>{@code refresh_token} — 用 refresh_token 续期</li>
     * </ul>
     */
    public static final String TOKEN_URL =
            BASE_URL + "/openapi/oauth/token?grant_type=client_credentials&scope=client";

    /**
     * IHR OAuth2 refresh_token 续期 URL（grant_type=refresh_token）
     */
    public static final String REFRESH_TOKEN_URL = BASE_URL + "/openapi/oauth/token";

    /**
     * 获取部门清单 v3 — 组织架构/部门
     */
    public static final String ORG_SEARCH_DEPARTMENT_URL =
            BASE_URL + "/openapi/thirdparty/api/org/v1/organizations/search";

    /**
     * access_token 有效期（秒）—— IHR 官方文档约定 2 小时
     */
    public static final long ACCESS_TOKEN_TTL_SECONDS = 7200L;

    /**
     * access_token Redis 缓存 TTL（秒）—— 比 IHR 返回的有效期短 5 分钟，提前刷新避免临界过期
     */
    public static final long ACCESS_TOKEN_CACHE_TTL_SECONDS = 6900L;

    /**
     * refresh_token 有效期（秒）—— IHR 官方文档约定 24 小时
     */
    public static final long REFRESH_TOKEN_TTL_SECONDS = 86400L;

    /**
     * refresh_token Redis 缓存 TTL（秒）—— 比 IHR 返回的有效期短 5 分钟
     */
    public static final long REFRESH_TOKEN_CACHE_TTL_SECONDS = 86100L;

    /**
     * searchType 取值 — 精确匹配
     */
    public static final String SEARCH_TYPE_EQUAL = "EQUAL";

    /**
     * searchType 取值 — 模糊匹配（LIKE）
     */
    public static final String SEARCH_TYPE_LIKE = "LIKE";

    /**
     * searchType 取值 — 模糊匹配（中文文档用「模糊」描述）
     */
    public static final String SEARCH_TYPE_FUZZY = "FUZZY";

    /**
     * searchType 取值 — IN 集合匹配
     */
    public static final String SEARCH_TYPE_IN = "IN";

    /**
     * searchKey 取值 — 部门 ID
     */
    public static final String SEARCH_KEY_DEPARTMENT_ID = "departmentId";

    /**
     * searchKey 取值 — 部门名称
     */
    public static final String SEARCH_KEY_DEPARTMENT_NAME = "departmentName";

    /**
     * searchKey 取值 — 部门编码
     */
    public static final String SEARCH_KEY_DEPARTMENT_CODE = "departmentCode";

    /**
     * searchKey 取值 — 上级部门 ID
     */
    public static final String SEARCH_KEY_PARENT_DEPARTMENT_ID = "parentDepartmentId";

    /**
     * 响应 code 成功值
     */
    public static final int RESPONSE_CODE_SUCCESS = 0;

    /**
     * 动态生成 IHR access_token 缓存 Key
     *
     * @return Redis Key：integration:ihr:token
     */
    public static String accessTokenRedisKey() {
        return RedisConstants.KEY_PREFIX + "ihr:token";
    }

    /**
     * 动态生成 IHR refresh_token 缓存 Key
     *
     * @return Redis Key：integration:ihr:refresh-token
     */
    public static String refreshTokenRedisKey() {
        return RedisConstants.KEY_PREFIX + "ihr:refresh-token";
    }
}
