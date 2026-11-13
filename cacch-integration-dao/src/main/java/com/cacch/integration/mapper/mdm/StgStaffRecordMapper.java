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
 * <p>手写 SQL 场景：</p>
 * <ul>
 *     <li>insertIgnore —— 入队幂等（UNIQUE 冲突时保留 ADD 优先级 + lastUpdate 取较大值）</li>
 *     <li>selectPendingBatch —— 用 FOR UPDATE SKIP LOCKED 防多实例并发捞取</li>
 *     <li>markProcessed —— 单条立即标记 PROCESSED</li>
 * </ul>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface StgStaffRecordMapper extends BaseMapper<StgStaffRecordDO> {

    /**
     * 幂等入队
     *
     * <p>UNIQUE(staff_id, pull_batch_no) 冲突时 DO UPDATE：</p>
     * <ul>
     *     <li>source：已存在 ADD → 保留 ADD；否则用新值</li>
     *     <li>lastUpdate：取冲突行与新入参中较大者（更新接口同一批次多次返回取最晚时间）</li>
     *     <li>pull_batch_no / updated_at：强制刷新（让冲突行保持活跃，下次 process 仍可捞到）</li>
     * </ul>
     *
     * @param id          雪花生成的内部主键
     * @param staffId     iHR 员工 UUID
     * @param source      ADD / UPDATE
     * @param lastUpdate  更新接口秒级时间戳（新增填 0）
     * @param pullBatchNo 拉取批次号
     */
    @Insert("INSERT INTO stg_staff_record (id, staff_id, source, last_update, pull_batch_no, status) "
            + "VALUES (#{id}, #{staffId}, #{source}, #{lastUpdate}, #{pullBatchNo}, 'PENDING') "
            + "ON CONFLICT (staff_id, pull_batch_no) DO UPDATE SET "
            + "    updated_at      = NOW(), "
            + "    pull_batch_no   = EXCLUDED.pull_batch_no, "
            + "    source          = CASE WHEN stg_staff_record.source = 'ADD' "
            + "                           THEN stg_staff_record.source "
            + "                           ELSE EXCLUDED.source END, "
            + "    last_update     = CASE WHEN stg_staff_record.source = 'ADD' "
            + "                           THEN stg_staff_record.last_update "
            + "                           ELSE GREATEST(COALESCE(stg_staff_record.last_update, 0), "
            + "                                        COALESCE(EXCLUDED.last_update, 0)) END")
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
     *     <li>UPDATE 内部按 last_update 秒级时间戳升序（早变更先落库，追溯顺序正确）</li>
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
