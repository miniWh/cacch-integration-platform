package com.cacch.integration.common.dto.heart;

/**
 * Heart 邮件拉取处理明细主体
 *
 * @param mailKey 去重键（Message-ID，缺失时为 {@code UID:{uid}}）
 * @param subject 邮件主题
 * @author hongfu_zhou@cacch.com
 */
public record HeartMailPullItem(String mailKey, String subject) {
}
