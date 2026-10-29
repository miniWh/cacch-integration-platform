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
     * 捞取待 CRM 回传记录（PENDING_CRM，附件已就绪）
     *
     * <p>回传阶段不做二次抢占，直接以捞取时的 version CAS 回写最终状态
     * （contractEdit 幂等，重复回传无副作用）。</p>
     *
     * @param limit 最大捞取条数，≤0 时返回空列表
     * @return 待 CRM 回传记录列表（updated_at 升序）；无数据时返回空列表（非 null）
     */
    List<OaContractAttachmentSyncDO> listPendingCrmForPush(int limit);

    /**
     * 回收超时 PROCESSING 记录为 RETRY
     *
     * <p>乐观锁抢占接入前正常运行不会产生 PROCESSING 记录，回收数恒为 0。</p>
     *
     * @param timeoutMinutes PROCESSING 超时阈值（分钟），≤0 时跳过
     * @return 实际回收的记录数
     */
    int reclaimTimeoutProcessing(int timeoutMinutes);

    /**
     * 乐观锁 CAS 抢占待处理记录（PENDING / RETRY → PROCESSING）
     *
     * @param id              中间记录主键
     * @param expectedVersion 捞取时读到的乐观锁版本号
     * @return true 表示抢占成功；false 表示并发冲突或状态已变化，调用方应跳过
     */
    boolean claimForProcess(Long id, Integer expectedVersion);

    /**
     * 附件就绪回填（PROCESSING → PENDING_CRM）
     *
     * @param id                 中间记录主键
     * @param expectedVersion    抢占后的乐观锁版本号
     * @param oaAttachmentId     OA 附件主键（ctp_attachment.ID）
     * @param attachmentFileId   附件文件标识（ctp_attachment.file_url）
     * @param attachmentFileName 附件文件名（ctp_attachment.filename）
     * @param attachmentUrl      回传 CRM 的附件下载地址
     * @return true 表示回填成功
     */
    boolean markAttachmentReady(Long id,
                                Integer expectedVersion,
                                String oaAttachmentId,
                                String attachmentFileId,
                                String attachmentFileName,
                                String attachmentUrl);

    /**
     * 业务跳过回写（PROCESSING → SKIPPED）
     *
     * @param id              中间记录主键
     * @param expectedVersion 抢占后的乐观锁版本号
     * @param skipReason      业务跳过原因
     * @return true 表示回写成功
     */
    boolean markSkipped(Long id, Integer expectedVersion, String skipReason);

    /**
     * 失败回写（PROCESSING → RETRY，retry_count 累加，不设上限）
     *
     * <p>失败原因自动截断至 {@code OaContractAttachmentConstants.MESSAGE_MAX_LENGTH}。</p>
     *
     * @param id              中间记录主键
     * @param expectedVersion 抢占后的乐观锁版本号
     * @param syncMessage     失败原因
     * @return true 表示回写成功
     */
    boolean markRetry(Long id, Integer expectedVersion, String syncMessage);

    /**
     * CRM 回传成功回写（PENDING_CRM → SUCCESS，终态）
     *
     * @param id              中间记录主键
     * @param expectedVersion 捞取时读到的乐观锁版本号
     * @param syncMessage     成功说明（如 return_code 与 msgId），超长自动截断
     * @return true 表示回写成功
     */
    boolean markSuccess(Long id, Integer expectedVersion, String syncMessage);

    /**
     * CRM 回传失败回写（PENDING_CRM → RETRY，retry_count 累加，不设上限）
     *
     * <p>失败原因自动截断至 {@code OaContractAttachmentConstants.MESSAGE_MAX_LENGTH}。</p>
     *
     * @param id              中间记录主键
     * @param expectedVersion 捞取时读到的乐观锁版本号
     * @param syncMessage     失败原因（如 CRM return_code / return_msg）
     * @return true 表示回写成功
     */
    boolean markCrmRetry(Long id, Integer expectedVersion, String syncMessage);

    /**
     * 存量数据补偿回写 CRM 合同数据 ID（仅补快照字段）
     *
     * <p>不累加 version、不改 sync_status，避免破坏 PENDING_CRM 阶段后续 CAS 状态回写。</p>
     *
     * @param id            中间记录主键
     * @param crmContractId CRM 合同数据 ID（formmain_2817.field0495），调用方保证非空
     * @return true 表示回写成功
     */
    boolean updateCrmContractId(Long id, String crmContractId);
}
