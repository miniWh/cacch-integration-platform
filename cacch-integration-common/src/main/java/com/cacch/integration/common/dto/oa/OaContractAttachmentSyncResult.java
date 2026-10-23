package com.cacch.integration.common.dto.oa;

import com.cacch.integration.common.dto.batch.BatchProcessResult;
import lombok.Getter;
import lombok.Setter;

/**
 * OA 货物购销合同附件回传 CRM 执行结果
 *
 * <p>成功 / 失败 / 跳过计数与明细由 {@link BatchProcessResult} 统一维护，
 * 本类仅补充执行层面的统计字段。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Setter
public class OaContractAttachmentSyncResult extends BatchProcessResult<OaContractAttachmentSyncItem> {

    /**
     * 本次是否真正执行；false 表示未获取到执行锁或功能已停用
     */
    private boolean executed = true;

    /**
     * 未执行原因（执行中 / 已停用 / 数据源未配置）；executed=true 时为空
     */
    private String message;

    /**
     * 本次由 OA 扫描新写入中间表的条数
     */
    private int scanned;

    /**
     * 本次从中间表捞取的待处理条数（PENDING + RETRY）
     */
    private int picked;
}
