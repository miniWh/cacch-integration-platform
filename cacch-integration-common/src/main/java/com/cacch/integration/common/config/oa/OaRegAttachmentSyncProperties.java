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
     * 单轮最多处理资料行数（仅指定 formMainId 单表扫描时生效）
     */
    private final int batchSize;

    /**
     * 单轮最多扫描登记报告主表（项目）数量；每个项目下资料行全部拉取
     */
    private final int formBatchSize;

    /**
     * 最大重试次数
     */
    private final int maxRetry;

    /**
     * 登记负责人姓名过滤（测试联调用）；非空时仅同步该负责人，如「李庆辉」
     */
    private final String ownerNameFilter;

    public OaRegAttachmentSyncProperties(Boolean enabled,
                                         String cron,
                                         Integer batchSize,
                                         Integer formBatchSize,
                                         Integer maxRetry,
                                         String ownerNameFilter) {
        this.enabled = enabled != null && enabled;
        this.cron = blankToDefault(cron, "0 */10 * * * ?");
        this.batchSize = batchSize != null && batchSize > 0
                ? batchSize
                : OaRegReportConstants.DEFAULT_BATCH_SIZE;
        this.formBatchSize = formBatchSize != null && formBatchSize > 0
                ? formBatchSize
                : OaRegReportConstants.DEFAULT_FORM_BATCH_SIZE;
        this.maxRetry = maxRetry != null && maxRetry > 0
                ? maxRetry
                : OaRegReportConstants.DEFAULT_MAX_RETRY;
        this.ownerNameFilter = ownerNameFilter != null ? ownerNameFilter.trim() : "";
    }

    /**
     * 是否启用了登记负责人过滤
     *
     * @return true 表示仅处理 {@link #ownerNameFilter} 指定负责人
     */
    public boolean hasOwnerNameFilter() {
        return !ownerNameFilter.isBlank();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
