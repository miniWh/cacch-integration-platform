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
     * （发起时间 / 流程状态 / NC 单号 / 销售订单编号 / 附件引用）与 updated_at，
     * 不覆盖 sync_status / retry_count / version 等同步状态字段，避免重置处理进度。</p>
     *
     * @param records 扫描结果记录；主键 id 须已由调用方预生成，同一 oa_form_main_id 不得重复
     * @return 受影响行数（含 INSERT 与 UPDATE 分支）
     */
    @Insert("<script>" +
            "INSERT INTO t_integration_oa_contract_attachment_sync " +
            "(id, oa_form_main_id, oa_start_date, flow_status, nc_order_no, oa_contract_no, " +
            "oa_attachment_ref, sync_status, retry_count, version, is_deleted) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.oaFormMainId}, #{item.oaStartDate,jdbcType=TIMESTAMP}, #{item.flowStatus}, " +
            "#{item.ncOrderNo}, #{item.oaContractNo}, #{item.oaAttachmentRef}, " +
            "'PENDING', 0, 0, 0)" +
            "</foreach> " +
            "ON CONFLICT (oa_form_main_id) WHERE is_deleted = 0 DO UPDATE SET " +
            "oa_start_date = EXCLUDED.oa_start_date, " +
            "flow_status = EXCLUDED.flow_status, " +
            "nc_order_no = EXCLUDED.nc_order_no, " +
            "oa_contract_no = EXCLUDED.oa_contract_no, " +
            "oa_attachment_ref = EXCLUDED.oa_attachment_ref, " +
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
}
