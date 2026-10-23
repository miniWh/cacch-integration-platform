package com.cacch.integration.common.enums.oa;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OA 货物购销合同附件回传 CRM 同步状态
 *
 * <p>REQ-OA-003：不设失败次数上限，失败统一停留在 {@link #RETRY} 持续重试，
 * 因此不存在 FAILED 终态。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum OaContractAttachmentSyncStatusEnum {

    /**
     * 待处理：已由 OA 扫描写入中间表
     */
    PENDING("PENDING", "待处理"),

    /**
     * 处理中：已被任务抢占，超过 processing-timeout 未更新则回收为 RETRY
     */
    PROCESSING("PROCESSING", "处理中"),

    /**
     * 已成功：CRM 合同修改成功，终态
     */
    SUCCESS("SUCCESS", "已成功"),

    /**
     * 失败待重试：已记录失败原因并告警，下次执行继续重试
     */
    RETRY("RETRY", "失败待重试"),

    /**
     * 附件就绪待回传：OA 附件已获取并回填中间表，等待 CRM 合同修改调用
     */
    PENDING_CRM("PENDING_CRM", "附件就绪待回传"),

    /**
     * 业务跳过：如 OA 附件缺失，终态
     */
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String desc;
}
