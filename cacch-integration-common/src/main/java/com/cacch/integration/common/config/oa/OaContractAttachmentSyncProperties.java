package com.cacch.integration.common.config.oa;

import com.cacch.integration.common.constant.oa.OaContractAttachmentConstants;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OA 货物购销合同附件回传 CRM 执行配置
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "oa.contract-attachment-sync")
public class OaContractAttachmentSyncProperties {

    /**
     * 是否启用扫描回传（紧急开关；false 时执行接口直接返回已停用）
     */
    private final boolean enabled;

    /**
     * NOT IN 排除列表分片大小，强制 ≤ {@link OaContractAttachmentConstants#MAX_NOT_IN_CHUNK}
     */
    private final int scanChunkSize;

    /**
     * 单次执行最多处理条数
     */
    private final int batchSize;

    public OaContractAttachmentSyncProperties(Boolean enabled, Integer scanChunkSize, Integer batchSize) {
        this.enabled = enabled == null || enabled;
        this.scanChunkSize = normalizeChunkSize(scanChunkSize);
        this.batchSize = batchSize != null && batchSize > 0
                ? batchSize
                : OaContractAttachmentConstants.DEFAULT_BATCH_SIZE;
    }

    /**
     * 分片大小收敛：下限 1，上限 {@link OaContractAttachmentConstants#MAX_NOT_IN_CHUNK}，默认 500
     *
     * @param scanChunkSize 配置值，可空
     * @return 收敛后的分片大小
     */
    private static int normalizeChunkSize(Integer scanChunkSize) {
        if (scanChunkSize == null || scanChunkSize < 1) {
            return OaContractAttachmentConstants.DEFAULT_SCAN_CHUNK_SIZE;
        }
        return Math.min(scanChunkSize, OaContractAttachmentConstants.MAX_NOT_IN_CHUNK);
    }
}
