package com.cacch.integration.mapper.oa;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.oa.OaContractAttachmentSyncDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
