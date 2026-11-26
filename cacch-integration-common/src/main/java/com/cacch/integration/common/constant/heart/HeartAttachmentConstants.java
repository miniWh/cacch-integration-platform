package com.cacch.integration.common.constant.heart;

/**
 * Heart 客户 PO 附件上传常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartAttachmentConstants {

    private HeartAttachmentConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "Heart客户PO附件上传业务";

    /**
     * 默认附件本地路径（固定值；后续由邮件解析结果替换）
     */
    public static final String DEFAULT_ATTACHMENT_FILE_PATH = "/home/zhf/app/测试图片.png";
}
