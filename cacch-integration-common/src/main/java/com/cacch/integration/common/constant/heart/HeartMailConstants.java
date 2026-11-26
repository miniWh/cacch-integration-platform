package com.cacch.integration.common.constant.heart;

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
     * 连接超时（毫秒）
     */
    public static final int CONNECTION_TIMEOUT_MS = 10000;

    /**
     * 读取超时（毫秒）
     */
    public static final int READ_TIMEOUT_MS = 10000;
}
