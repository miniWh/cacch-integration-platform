package com.cacch.integration.common.constant.keruyun;

import com.cacch.integration.common.constant.redis.RedisConstants;

/**
 * 客如云开放平台业务常量
 *
 * <p>签名规则见客如云 V2 文档：公共参数（sign 除外）按参数名 ASCII 升序拼接为
 * {@code k1v1k2v2...}，再追加 body 与 token 后做 SHA-256（小写 hex）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class KeruyunConstants {

    private KeruyunConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "客如云业务日志";

    /**
     * 固定版本号（客如云 V2 签名规则要求）
     */
    public static final String DEFAULT_VERSION = "2.0";

    /**
     * Token Redis 缓存默认 TTL（秒）：客如云门店 token 当前固定不变，默认缓存 24 小时
     */
    public static final long TOKEN_DEFAULT_TTL_SECONDS = 86400L;

    /**
     * 公共参数名：开发者 appKey
     */
    public static final String PARAM_APP_KEY = "appKey";

    /**
     * 公共参数名：门店 ID（门店授权维度）
     */
    public static final String PARAM_SHOP_IDENTY = "shopIdenty";

    /**
     * 公共参数名：接口版本
     */
    public static final String PARAM_VERSION = "version";

    /**
     * 公共参数名：秒级时间戳
     */
    public static final String PARAM_TIMESTAMP = "timestamp";

    /**
     * 公共参数名：签名
     */
    public static final String PARAM_SIGN = "sign";

    /**
     * 公共参数名：门店 token（业务请求通过 URL 参数传递）
     */
    public static final String PARAM_TOKEN = "token";

    /**
     * 签名串中 body 的前缀字面量（直接追加 "body" + JSON 报文）
     */
    public static final String BODY_PREFIX = "body";

    /**
     * 获取门店 token 接口路径（GET）
     */
    public static final String PATH_GET_TOKEN = "/open/v1/token/get";

    /**
     * 接口成功返回码
     */
    public static final int SUCCESS_CODE = 0;

    /**
     * 客如云 Token Redis Key
     *
     * @param appKey     开发者 appKey
     * @param shopIdenty 门店 ID
     * @return 形如 {@code integration:keruyun:token:{appKey}:{shopIdenty}} 的缓存 Key
     */
    public static String tokenRedisKey(String appKey, String shopIdenty) {
        return RedisConstants.KEY_PREFIX + "keruyun:token:" + appKey + ":" + shopIdenty;
    }
}
