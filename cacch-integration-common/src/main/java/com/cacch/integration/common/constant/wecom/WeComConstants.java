package com.cacch.integration.common.constant.wecom;

import com.cacch.integration.common.constant.redis.RedisConstants;

/**
 * 企业微信相关常量
 *
 * @author hongfu_zhou@cacch.com
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
     * 智能表格 — 添加记录 API 地址
     */
    public static final String SMARTSHEET_ADD_RECORDS_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/wedoc/smartsheet/add_records?access_token=%s";

    /**
     * 智能表格 — 更新记录 API 地址
     */
    public static final String SMARTSHEET_UPDATE_RECORDS_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/wedoc/smartsheet/update_records?access_token=%s";

    /**
     * 文档 — 新建文档/智能表格 API 地址
     */
    public static final String DOC_CREATE_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/wedoc/create_doc?access_token=%s";

    /**
     * 智能表格文档类型（doc_type=10）
     */
    public static final int DOC_TYPE_SMART_SHEET = 10;

    /**
     * 智能表格记录 key 类型 — 字段 ID
     */
    public static final String CELL_VALUE_KEY_TYPE_FIELD_ID = "CELL_VALUE_KEY_TYPE_FIELD_ID";

    /**
     * 企微预约会议最小时长（秒），官方要求不少于 300 秒
     */
    public static final int MEETING_MIN_DURATION_SECONDS = 300;

    /**
     * 会议 — 创建预约会议 API 地址
     */
    public static final String MEETING_CREATE_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/meeting/create?access_token=%s";

    /**
     * 会议 — 获取会议详情 API 地址
     */
    public static final String MEETING_GET_INFO_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/meeting/get_info?access_token=%s";

    /**
     * 会议 — 获取录制转写详情 API 地址
     */
    public static final String MEETING_TRANSCRIPT_GET_DETAIL_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/meeting/record/transcript/get_detail?access_token=%s";

    /**
     * 群机器人 Webhook 发送消息 API 地址（key 由配置注入）
     */
    public static final String WEBHOOK_SEND_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=%s";

    /**
     * Webhook 告警去重 Redis Key
     *
     * @param alertKey 告警标识（任务名 + 错误摘要 hash）
     */
    public static String webhookAlertRedisKey(String alertKey) {
        return RedisConstants.KEY_PREFIX + "wecom:webhook:alert:" + alertKey;
    }

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
