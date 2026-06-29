package com.cacch.integration.common.constant.wecom;

import com.cacch.integration.common.constant.redis.RedisConstants;

/**
 * 企业微信相关常量
 *
 * @author cacch-integration
 */
public final class WeComConstants {

    private WeComConstants() {
    }

    /**
     * 自建应用默认 app-key（对应 wecom.apps 配置项）
     */
    public static final String SELF_BUILT_APP_KEY = "self-built";

    /**
     * access_token 缓存有效期（秒），设为 7000s（企微返回 7200s，提前 200s 刷新防过期窗口）
     */
    public static final long TOKEN_TTL_SECONDS = 7000L;

    /**
     * access_token 获取 API 地址
     */
    public static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";

    /**
     * 智能表格 — 查询子表 API 地址
     */
    public static final String SMARTSHEET_GET_SHEET_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/wedoc/smartsheet/get_sheet?access_token=%s";

    /**
     * 智能表格 — 查询字段 API 地址
     */
    public static final String SMARTSHEET_GET_FIELDS_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/wedoc/smartsheet/get_fields?access_token=%s";

    /**
     * 智能表格 — 查询记录 API 地址
     */
    public static final String SMARTSHEET_GET_RECORDS_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/wedoc/smartsheet/get_records?access_token=%s";

    /**
     * 动态生成企业微信 access_token 缓存 Key
     *
     * @param corpid 企业 ID
     * @param appKey 业务标识（如 self-built、address-book）
     * @return Redis Key：integration:wecom:token:{corpid}:{appKey}
     */
    public static String tokenRedisKey(String corpid, String appKey) {
        return RedisConstants.KEY_PREFIX + "wecom:token:" + corpid + ":" + appKey;
    }
}
