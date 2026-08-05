package com.cacch.integration.common.constant.fdd;

import com.cacch.integration.common.constant.redis.RedisConstants;

/**
 * 法大大业务常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class FddConstants {

    private FddConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "Fdd";

    /**
     * 法大大业务接口成功返回码
     */
    public static final int SUCCESS_CODE = 100000;

    /**
     * OAuth2 Token 接口成功返回码（与 ESB/接口文档一致）
     */
    public static final int TOKEN_SUCCESS_CODE = 0;

    /**
     * OAuth2 加密模式时间戳格式（文档要求，非 Unix 毫秒）
     */
    public static final String TOKEN_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    /**
     * 企业认证渠道：标准实名认证
     */
    public static final int VERIFIED_CHANNEL_STANDARD = 0;

    /**
     * 首次认证
     */
    public static final int FIRST_VERIFY = 1;

    /**
     * 重新认证
     */
    public static final int REPEAT_VERIFY = 2;

    /**
     * 企业管理员身份：全部
     */
    public static final int APPLICATION_TYPE_ALL = 0;

    /**
     * 是否发送短信：否
     */
    public static final int SEND_SMS_NO = 0;

    /**
     * 页面信息是否可修改：不允许
     */
    public static final int PAGE_MODIFY_FORBIDDEN = 2;

    /**
     * 企业回调通知类型
     */
    public static final String NOTIFY_TYPE_ENTERPRISE = "ENTERPRISE_IDENTIFY";

    /**
     * 个人回调通知类型
     */
    public static final String NOTIFY_TYPE_PERSON = "PERSONAL_IDENTIFY";

    /**
     * 企业回调：已认证
     */
    public static final int ENTERPRISE_STATUS_CERTIFIED = 3;

    /**
     * 企业回调：认证失败
     */
    public static final int ENTERPRISE_STATUS_FAILED = 4;

    /**
     * 个人回调：已认证
     */
    public static final int PERSON_STATUS_CERTIFIED = 1;

    /**
     * 个人回调：认证失败
     */
    public static final int PERSON_STATUS_FAILED = 2;

    /**
     * 证件类型：身份证
     */
    public static final String CERT_TYPE_ID_CARD = "0";

    /**
     * 是否支持其他证件：否（仅身份证）
     */
    public static final int OTHER_CERT_TYPE_NO = 0;

    /**
     * 非小程序
     */
    public static final int MINI_PROGRAM_NO = 0;

    /**
     * OAuth2 Token Redis Key
     *
     * @param appId 法大大应用 ID
     * @return Redis Key
     */
    public static String tokenRedisKey(String appId) {
        return RedisConstants.KEY_PREFIX + "fdd:token:" + appId;
    }
}
