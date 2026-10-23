package com.cacch.integration.service.oa.api;

import com.cacch.integration.entity.oa.OaContractAttachmentSyncDO;
import com.cacch.integration.integration.oa.client.dto.OaContractRow;

import java.util.List;

/**
 * OA 货物购销合同附件回传 CRM 中间记录服务
 *
 * <p>负责中间记录表的读写：扫描入库、待处理捞取、状态回写与同步状态查询。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaContractAttachmentSyncService {

    /**
     * 查询中间表已入库的全部 OA 购销合同主表 ID（NOT IN 增量扫描排除列表数据源）
     *
     * @return 已入库 oa_form_main_id 列表；无数据时返回空列表（非 null）
     */
    List<Long> listExistingFormMainIds();

    /**
     * 扫描结果幂等写入中间表
     *
     * <p>按 oa_form_main_id 去重后批量 UPSERT：不存在则新增（PENDING），已存在仅刷新
     * OA 快照字段，不覆盖同步状态。</p>
     *
     * @param rows OA 购销合同扫描行，可空
     * @return 本次写入的记录数（去重后）
     */
    int upsertFromScan(List<OaContractRow> rows);

    /**
     * 捞取待处理记录
     *
     * <p>仅捞取 PENDING / RETRY 记录，不设失败次数上限（retry_count 仅用于观测）。</p>
     *
     * @param limit 最大捞取条数，≤0 时返回空列表
     * @return 待处理记录列表（updated_at 升序）；无数据时返回空列表（非 null）
     */
    List<OaContractAttachmentSyncDO> listPendingForProcess(int limit);

    /**
     * 回收超时 PROCESSING 记录为 RETRY
     *
     * <p>乐观锁抢占接入前正常运行不会产生 PROCESSING 记录，回收数恒为 0。</p>
     *
     * @param timeoutMinutes PROCESSING 超时阈值（分钟），≤0 时跳过
     * @return 实际回收的记录数
     */
    int reclaimTimeoutProcessing(int timeoutMinutes);
}
