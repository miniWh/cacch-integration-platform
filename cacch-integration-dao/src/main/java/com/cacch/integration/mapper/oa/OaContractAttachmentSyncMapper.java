package com.cacch.integration.mapper.oa;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.oa.OaContractAttachmentSyncDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OA 货物购销合同附件回传 CRM 中间记录表 Mapper
 *
 * <p>基础 CRUD 继承自 {@link BaseMapper}；业务唯一键为 {@code oa_form_main_id}
 * （一个合同只有一个附件），扫描入库通过 PostgreSQL
 * {@code INSERT ... ON CONFLICT (oa_form_main_id) WHERE is_deleted = 0 DO UPDATE}
 * 原子语义实现幂等写入。主键 {@code id} 由 Service 层调用 {@code IdWorker} 预生成
 * （手写 INSERT 不触发 {@code ASSIGN_ID}）。</p>
 *
 * <p>ON CONFLICT 必须携带部分唯一索引谓词 {@code WHERE is_deleted = 0}
 * （见 V31 脚本 {@code uk_oa_contract_attachment_sync_form_main}），否则无法推断冲突目标。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface OaContractAttachmentSyncMapper extends BaseMapper<OaContractAttachmentSyncDO> {

    /**
     * 查询中间表已入库的全部 OA 购销合同主表 ID（NOT IN 排除列表数据源）
     *
     * @return 已入库 oa_form_main_id 列表；无数据时返回空列表（非 null）
     */
    @Select("SELECT oa_form_main_id FROM t_integration_oa_contract_attachment_sync WHERE is_deleted = 0")
    List<Long> selectExistingFormMainIds();

    /**
     * 批量幂等写入扫描结果 —— INSERT ON CONFLICT (oa_form_main_id) DO UPDATE
     *
     * <p>记录不存在时 INSERT（状态 PENDING）；已存在时仅刷新 OA 快照字段
     * （发起时间 / 流程状态 / NC 单号 / 销售订单编号 / 附件引用 / CRM 合同 ID）与 updated_at，
     * 不覆盖 sync_status / retry_count / version 等同步状态字段，避免重置处理进度。</p>
     *
     * @param records 扫描结果记录；主键 id 须已由调用方预生成，同一 oa_form_main_id 不得重复
     * @return 受影响行数（含 INSERT 与 UPDATE 分支）
     */
    @Insert("<script>" +
            "INSERT INTO t_integration_oa_contract_attachment_sync " +
            "(id, oa_form_main_id, oa_start_date, flow_status, nc_order_no, oa_contract_no, " +
            "oa_attachment_ref, crm_contract_id, sync_status, retry_count, version, is_deleted) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.oaFormMainId}, #{item.oaStartDate,jdbcType=TIMESTAMP}, #{item.flowStatus}, " +
            "#{item.ncOrderNo}, #{item.oaContractNo}, #{item.oaAttachmentRef}, #{item.crmContractId}, " +
            "'PENDING', 0, 0, 0)" +
            "</foreach> " +
            "ON CONFLICT (oa_form_main_id) WHERE is_deleted = 0 DO UPDATE SET " +
            "oa_start_date = EXCLUDED.oa_start_date, " +
            "flow_status = EXCLUDED.flow_status, " +
            "nc_order_no = EXCLUDED.nc_order_no, " +
            "oa_contract_no = EXCLUDED.oa_contract_no, " +
            "oa_attachment_ref = EXCLUDED.oa_attachment_ref, " +
            "crm_contract_id = EXCLUDED.crm_contract_id, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE t_integration_oa_contract_attachment_sync.is_deleted = 0" +
            "</script>")
    int upsertBatch(@Param("list") List<OaContractAttachmentSyncDO> records);

    /**
     * 捞取待处理记录（PENDING / RETRY，不设失败次数上限）
     *
     * <p>不校验 retry_count（仅观测字段），单次捞取条数由调用方限制；
     * 按 updated_at 升序优先处理最早写入的数据。</p>
     *
     * @param limit 最大捞取条数
     * @return 待处理记录列表；无数据时返回空列表（非 null）
     */
    @Select("SELECT * FROM t_integration_oa_contract_attachment_sync " +
            "WHERE sync_status IN ('PENDING', 'RETRY') " +
            "AND is_deleted = 0 " +
            "ORDER BY updated_at ASC, id ASC " +
            "LIMIT #{limit}")
    List<OaContractAttachmentSyncDO> selectPendingForProcess(@Param("limit") int limit);

    /**
     * 回收超时 PROCESSING 记录为 RETRY
     *
     * <p>PROCESSING 表示已被任务抢占正在处理；超过阈值仍未回写最终状态的记录视为
     * 任务异常遗留的僵尸记录，回收为 RETRY 以便下次执行重新捞取。乐观锁抢占（F8）
     * 接入前正常运行不会产生 PROCESSING 记录，本方法回收数恒为 0。</p>
     *
     * @param threshold 回收阈值时间点，last_sync_at 早于该时间点的 PROCESSING 记录被回收
     * @return 实际回收的记录数
     */
    @Update("UPDATE t_integration_oa_contract_attachment_sync " +
            "SET sync_status = 'RETRY', version = version + 1, updated_at = CURRENT_TIMESTAMP " +
            "WHERE sync_status = 'PROCESSING' " +
            "AND is_deleted = 0 " +
            "AND last_sync_at IS NOT NULL " +
            "AND last_sync_at < #{threshold}")
    int reclaimTimeoutProcessing(@Param("threshold") LocalDateTime threshold);

    /**
     * 乐观锁 CAS 抢占待处理记录（PENDING / RETRY → PROCESSING）
     *
     * <p>以 id + version + 状态集合为条件原子更新，更新行数为 0 表示记录已被
     * 其他执行轮次抢走或状态已变化，调用方应跳过该记录。</p>
     *
     * @param id              中间记录主键
     * @param expectedVersion 捞取时读到的乐观锁版本号
     * @return 1 表示抢占成功；0 表示抢占失败（并发冲突 / 状态已变化）
     */
    @Update("UPDATE t_integration_oa_contract_attachment_sync " +
            "SET sync_status = 'PROCESSING', version = version + 1, " +
            "last_sync_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} " +
            "AND version = #{expectedVersion} " +
            "AND sync_status IN ('PENDING', 'RETRY') " +
            "AND is_deleted = 0")
    int claimForProcess(@Param("id") Long id, @Param("expectedVersion") Integer expectedVersion);

    /**
     * 附件就绪回填（PROCESSING → PENDING_CRM）
     *
     * <p>回填 OA 附件信息与下载地址，状态置为待 CRM 回传；抢占失败（非 PROCESSING）
     * 或版本不匹配时不更新。</p>
     *
     * @param id                 中间记录主键
     * @param expectedVersion    抢占后的乐观锁版本号
     * @param oaAttachmentId     OA 附件主键（ctp_attachment.ID）
     * @param attachmentFileId   附件文件标识（ctp_attachment.file_url）
     * @param attachmentFileName 附件文件名（ctp_attachment.filename）
     * @param attachmentUrl      回传 CRM 的附件下载地址
     * @return 1 表示回填成功；0 表示状态/版本不匹配未更新
     */
    @Update("UPDATE t_integration_oa_contract_attachment_sync " +
            "SET oa_attachment_id = #{oaAttachmentId}, " +
            "attachment_file_id = #{attachmentFileId}, " +
            "attachment_file_name = #{attachmentFileName}, " +
            "attachment_url = #{attachmentUrl}, " +
            "sync_status = 'PENDING_CRM', " +
            "skip_reason = NULL, " +
            "version = version + 1, " +
            "last_sync_at = CURRENT_TIMESTAMP, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} " +
            "AND version = #{expectedVersion} " +
            "AND sync_status = 'PROCESSING' " +
            "AND is_deleted = 0")
    int markAttachmentReady(@Param("id") Long id,
                            @Param("expectedVersion") Integer expectedVersion,
                            @Param("oaAttachmentId") String oaAttachmentId,
                            @Param("attachmentFileId") String attachmentFileId,
                            @Param("attachmentFileName") String attachmentFileName,
                            @Param("attachmentUrl") String attachmentUrl);

    /**
     * 业务跳过回写（PROCESSING → SKIPPED）
     *
     * <p>用于按附件引用查不到 OA 附件等业务性终止场景，仅记录原因不参与重试。</p>
     *
     * @param id              中间记录主键
     * @param expectedVersion 抢占后的乐观锁版本号
     * @param skipReason      业务跳过原因
     * @return 1 表示回写成功；0 表示状态/版本不匹配未更新
     */
    @Update("UPDATE t_integration_oa_contract_attachment_sync " +
            "SET sync_status = 'SKIPPED', " +
            "skip_reason = #{skipReason}, " +
            "version = version + 1, " +
            "last_sync_at = CURRENT_TIMESTAMP, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} " +
            "AND version = #{expectedVersion} " +
            "AND sync_status = 'PROCESSING' " +
            "AND is_deleted = 0")
    int markSkipped(@Param("id") Long id,
                    @Param("expectedVersion") Integer expectedVersion,
                    @Param("skipReason") String skipReason);

    /**
     * 失败回写（PROCESSING → RETRY，retry_count 累加，不设上限）
     *
     * <p>用于处理过程异常场景；记录保持 RETRY，下次执行继续捞取重试。</p>
     *
     * @param id              中间记录主键
     * @param expectedVersion 抢占后的乐观锁版本号
     * @param syncMessage     失败原因（调用方须已截断至 500 字符内）
     * @return 1 表示回写成功；0 表示状态/版本不匹配未更新
     */
    @Update("UPDATE t_integration_oa_contract_attachment_sync " +
            "SET sync_status = 'RETRY', " +
            "sync_message = #{syncMessage}, " +
            "retry_count = retry_count + 1, " +
            "version = version + 1, " +
            "last_sync_at = CURRENT_TIMESTAMP, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} " +
            "AND version = #{expectedVersion} " +
            "AND sync_status = 'PROCESSING' " +
            "AND is_deleted = 0")
    int markRetry(@Param("id") Long id,
                  @Param("expectedVersion") Integer expectedVersion,
                  @Param("syncMessage") String syncMessage);

    /**
     * 捞取待 CRM 回传记录（PENDING_CRM，附件已就绪）
     *
     * <p>PENDING_CRM 表示 OA 附件已获取并上传文件服务器，仅剩 CRM contractEdit 回传；
     * 回传阶段不做二次抢占（contractEdit 幂等，重复写同一附件 URL 无副作用），
     * 直接以捞取时的 version CAS 回写最终状态。</p>
     *
     * @param limit 最大捞取条数
     * @return 待 CRM 回传记录列表；无数据时返回空列表（非 null）
     */
    @Select("SELECT * FROM t_integration_oa_contract_attachment_sync " +
            "WHERE sync_status = 'PENDING_CRM' " +
            "AND is_deleted = 0 " +
            "ORDER BY updated_at ASC, id ASC " +
            "LIMIT #{limit}")
    List<OaContractAttachmentSyncDO> selectPendingCrmForPush(@Param("limit") int limit);

    /**
     * CRM 回传成功回写（PENDING_CRM → SUCCESS，终态）
     *
     * @param id              中间记录主键
     * @param expectedVersion 捞取时读到的乐观锁版本号
     * @param syncMessage     成功说明（如 return_code 与 msgId）
     * @return 1 表示回写成功；0 表示状态/版本不匹配未更新
     */
    @Update("UPDATE t_integration_oa_contract_attachment_sync " +
            "SET sync_status = 'SUCCESS', " +
            "sync_message = #{syncMessage}, " +
            "skip_reason = NULL, " +
            "version = version + 1, " +
            "last_sync_at = CURRENT_TIMESTAMP, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} " +
            "AND version = #{expectedVersion} " +
            "AND sync_status = 'PENDING_CRM' " +
            "AND is_deleted = 0")
    int markSuccess(@Param("id") Long id,
                    @Param("expectedVersion") Integer expectedVersion,
                    @Param("syncMessage") String syncMessage);

    /**
     * CRM 回传失败回写（PENDING_CRM → RETRY，retry_count 累加，不设上限）
     *
     * <p>用于 contractEdit 返回非 0 或调用异常场景；记录保持 RETRY，
     * 下次执行由主捞取入口（PENDING / RETRY）重新捞取，完整重走附件获取与回传链路。</p>
     *
     * @param id              中间记录主键
     * @param expectedVersion 捞取时读到的乐观锁版本号
     * @param syncMessage     失败原因（调用方须已截断至 500 字符内）
     * @return 1 表示回写成功；0 表示状态/版本不匹配未更新
     */
    @Update("UPDATE t_integration_oa_contract_attachment_sync " +
            "SET sync_status = 'RETRY', " +
            "sync_message = #{syncMessage}, " +
            "retry_count = retry_count + 1, " +
            "version = version + 1, " +
            "last_sync_at = CURRENT_TIMESTAMP, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} " +
            "AND version = #{expectedVersion} " +
            "AND sync_status = 'PENDING_CRM' " +
            "AND is_deleted = 0")
    int markCrmRetry(@Param("id") Long id,
                     @Param("expectedVersion") Integer expectedVersion,
                     @Param("syncMessage") String syncMessage);

    /**
     * 存量数据补偿回写 CRM 合同数据 ID（仅补快照字段）
     *
     * <p>field0495 标量子查询上线前入库的记录 {@code crm_contract_id} 为空，且 NOT IN 增量
     * 扫描不会重刷已入库记录；处理链路反查 OA 命中后调用本方法回写。<strong>注意</strong>：
     * 不累加 version、不改 sync_status，避免破坏 PENDING_CRM 阶段后续以捞取 version 进行的
     * CAS 状态回写（markSuccess / markCrmRetry）。</p>
     *
     * @param id            中间记录主键
     * @param crmContractId CRM 合同数据 ID（formmain_2817.field0495）
     * @return 1 表示回写成功；0 表示记录不存在或已逻辑删除
     */
    @Update("UPDATE t_integration_oa_contract_attachment_sync " +
            "SET crm_contract_id = #{crmContractId}, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} " +
            "AND is_deleted = 0")
    int updateCrmContractId(@Param("id") Long id,
                            @Param("crmContractId") String crmContractId);
}
