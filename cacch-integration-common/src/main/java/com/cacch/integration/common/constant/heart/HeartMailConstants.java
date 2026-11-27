package com.cacch.integration.common.constant.heart;

import java.time.LocalTime;

/**
 * Heart 邮件拉取业务参数常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartMailConstants {

    private HeartMailConstants() {
    }

    /**
     * IMAP 协议（SSL 收信）
     */
    public static final String IMAP_PROTOCOL = "imaps";

    /**
     * 默认邮件文件夹
     */
    public static final String DEFAULT_FOLDER = "INBOX";

    /**
     * 单轮最多处理邮件数
     */
    public static final Integer DEFAULT_MAX_PER_ROUND = 20;

    /**
     * 缺省结束时间（当天 23:59:59）
     */
    public static final LocalTime DEFAULT_END_TIME = LocalTime.of(23, 59, 59);

    /**
     * 连接超时（毫秒）
     */
    public static final int CONNECTION_TIMEOUT_MS = 10000;

    /**
     * 读取超时（毫秒）
     */
    public static final int READ_TIMEOUT_MS = 10000;

    /**
     * Message-ID 缺失时去重键前缀
     */
    public static final String MAIL_KEY_UID_PREFIX = "UID:";

    /**
     * 正文单段最大字符数（超出截断）
     */
    public static final int MAX_BODY_CHARS = 1_000_000;

    /**
     * 邮件拉取最大重试次数
     */
    public static final int MAX_RETRY = 3;

    /**
     * Heart 发件人白名单地址
     */
    public static final String HEART_SENDER_ADDRESS = "ycho@heart-jp.com";
}
