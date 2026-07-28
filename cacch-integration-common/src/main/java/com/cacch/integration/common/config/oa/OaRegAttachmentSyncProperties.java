package com.cacch.integration.common.config.oa;

import com.cacch.integration.common.constant.oa.OaRegReportConstants;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 国内登记报告附件同步定时任务配置
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "oa.attachment-sync")
public class OaRegAttachmentSyncProperties {

    /**
     * 是否启用定时同步
     */
    private final boolean enabled;

    /**
     * cron 表达式
     */
    private final String cron;

    /**
     * 单轮最多处理资料行数
     */
    private final int batchSize;

    /**
     * 最大重试次数
     */
    private final int maxRetry;

    public OaRegAttachmentSyncProperties(Boolean enabled,
                                         String cron,
                                         Integer batchSize,
                                         Integer maxRetry) {
        this.enabled = enabled != null && enabled;
        this.cron = blankToDefault(cron, "0 */10 * * * ?");
        this.batchSize = batchSize != null && batchSize > 0
                ? batchSize
                : OaRegReportConstants.DEFAULT_BATCH_SIZE;
        this.maxRetry = maxRetry != null && maxRetry > 0
                ? maxRetry
                : OaRegReportConstants.DEFAULT_MAX_RETRY;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
