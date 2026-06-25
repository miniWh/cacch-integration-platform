package com.cacch.integration.common.constant;

/**
 * 集成模块常量定义
 *
 * @author cacch-integration
 */
public final class IntegrationConstants {

    private IntegrationConstants() {
    }

    // ———————————————— Redis Key 前缀 ————————————————

    /**
     * 集成模块 Redis Key 前缀
     */
    public static final String REDIS_KEY_PREFIX = "integration:";

    // ———————————————— 企业微信 ————————————————

    /**
     * 动态生成企业微信 access_token 缓存 Key
     *
     * @param corpid 企业 ID
     * @param appKey 业务标识（如 address-book、customer-contact）
     * @return Redis Key：integration:wecom:token:{corpid}:{appKey}
     */
    public static String wecomTokenKey(String corpid, String appKey) {
        return REDIS_KEY_PREFIX + "wecom:token:" + corpid + ":" + appKey;
    }

    /**
     * access_token 缓存有效期（秒），设为 7000s（企微返回 7200s，提前 200s 刷新防过期窗口）
     */
    public static final long WECOM_TOKEN_TTL_SECONDS = 7000L;

    /**
     * 企微 access_token 获取 API 地址
     */
    public static final String WECOM_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
}
