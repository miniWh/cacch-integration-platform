package com.cacch.integration.common.constant.heart;

/**
 * Heart 邮件流程状态常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartMailProcessStatusConstants {

    private HeartMailProcessStatusConstants() {
    }

    /**
     * 邮件正文与附件已完整入库，待解析
     */
    public static final String MAIL_RECEIVED = "MAIL_RECEIVED";

    /**
     * 非 Heart 邮件（发件人白名单外），不进入解析主流程
     */
    public static final String SKIP = "SKIP";

    /**
     * 拉取或附件落盘失败，等待重试
     */
    public static final String FAILED = "FAILED";

    /**
     * 重试超次，等待人工处理
     */
    public static final String MANUAL_REVIEW = "MANUAL_REVIEW";
}
