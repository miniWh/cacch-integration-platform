package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.StgStaffRecordDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * iHR 增量员工暂存队列 Mapper
 *
 * <ul>
 *     <li>source='ADD' → 部分唯一索引 ON(staff_id) WHERE source='ADD' — 新增只发生一次</li>
 *     <li>source='UPDATE' → 部分唯一索引 ON(staff_id, last_update) WHERE source='UPDATE' — 不同变更版本各一行</li>
 *     <li>ON CONFLICT DO NOTHING —— 任何唯一约束冲突直接跳过，零写放大</li>
 * </ul>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface StgStaffRecordMapper extends BaseMapper<StgStaffRecordDO> {

    /**
     * 幂等入队
     *
     * <p>依赖表上的两个部分唯一索引（add 按 staff_id / update 按 staff_id+last_update），
     * 任何冲突都 DO NOTHING —— 无新变更 → 不动；有新变更（last_update 变大）→ 新行插入。</p>
     *
     * @param id          雪花生成的内部主键
     * @param staffId     iHR 员工 UUID
     * @param source      ADD / UPDATE
     * @param lastUpdate  毫秒时间戳（additions 固定传 0）
     * @param pullBatchNo 拉取批次号（审计用，不参与唯一约束）
     */
    @Insert("INSERT INTO stg_staff_record (id, staff_id, source, last_update, pull_batch_no, status) "
            + "VALUES (#{id}, #{staffId}, #{source}, COALESCE(#{lastUpdate}, 0), #{pullBatchNo}, 'PENDING') "
            + "ON CONFLICT DO NOTHING")
    int insertIgnore(@Param("id") Long id,
                     @Param("staffId") String staffId,
                     @Param("source") String source,
                     @Param("lastUpdate") Long lastUpdate,
                     @Param("pullBatchNo") String pullBatchNo);

    /**
     * 捞取待处理批次 —— FOR UPDATE SKIP LOCKED 防多实例并发
     *
     * <p>排序规则保证业务正确性：</p>
     * <ol>
     *     <li>source = 'ADD' 必须先于 'UPDATE' 处理（新员工先建档，拉链才能正确关链新开）</li>
     *     <li>UPDATE 内部按 last_update 升序（早变更先落库，追溯顺序正确）</li>
     * </ol>
     *
     * @param limit 单次捞取上限（默认 500）
     * @return 锁已拿到的待处理行列表；空列表表示本轮无待处理数据
     */
    @Select("SELECT id, staff_id, source, last_update, pull_batch_no, process_batch_no, status, "
            + "       created_at, updated_at, is_deleted "
            + "FROM stg_staff_record "
            + "WHERE status = 'PENDING' "
            + "  AND is_deleted = 0 "
            + "ORDER BY "
            + "    CASE source WHEN 'ADD' THEN 1 ELSE 2 END ASC, "
            + "    COALESCE(last_update, 0) ASC, "
            + "    id ASC "
            + "LIMIT #{limit} "
            + "FOR UPDATE SKIP LOCKED")
    List<StgStaffRecordDO> selectPendingBatch(@Param("limit") int limit);

    /**
     * 单条标记 PROCESSED
     *
     * <p>Process 任务单条处理完立即标记，避免中间挂掉时已处理行重跑。</p>
     *
     * @param id             行主键
     * @param processBatchNo 处理批次号
     */
    @Update("UPDATE stg_staff_record SET "
            + "    status           = 'PROCESSED', "
            + "    process_batch_no = #{processBatchNo}, "
            + "    updated_at       = NOW() "
            + "WHERE id = #{id}")
    int markProcessed(@Param("id") Long id,
                      @Param("processBatchNo") String processBatchNo);
}
