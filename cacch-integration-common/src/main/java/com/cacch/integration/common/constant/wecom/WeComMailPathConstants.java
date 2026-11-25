package com.cacch.integration.common.constant.wecom;

/**
 * 企业微信邮件接口地址常量
 *
 * <p>企微 API 域名固定为 qyapi.weixin.qq.com，access_token 内嵌在 URL query 参数中，
 * 故此处直接声明完整 URL 模板（%s 为 access_token 占位符）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class WeComMailPathConstants {

    private WeComMailPathConstants() {
    }

    /**
     * 获取收件箱邮件列表 API 地址（POST）
     */
    public static final String GET_MAIL_LIST_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/exmail/app/get_mail_list?access_token=%s";
}
