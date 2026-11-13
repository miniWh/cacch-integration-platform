package com.cacch.integration.service.mdm.api;

import com.cacch.integration.entity.mdm.StgStaffRecordDO;

import java.util.List;

/**
 * MDM 增量员工暂存队列表业务 Service 接口
 *
 * <p>封装 stg_staff_record 表的所有读写业务逻辑：入队幂等、捞取批次、处理状态流转。
 * 上接 Manager 编排层，下接 Mapper 持久层。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMdmEmpIncrementalQueueService {

    /**
     * 单条幂等入队
     *
     * <p>UNIQUE(staff_id, pull_batch_no) 冲突时自动 DO UPDATE：
     * 已存在 ADD 保留 ADD；lastUpdate 取较大值。</p>
     *
     * @param staffId     iHR 员工 UUID
     * @param source      ADD / UPDATE
     * @param lastUpdate  更新接口秒级时间戳（新增填 0）
     * @param pullBatchNo 拉取批次号
     * @return 实际写入行数（0 = 冲突且未更新，1 = 新插入或 DO UPDATE）
     */
    int enqueueOne(String staffId, String source, Long lastUpdate, String pullBatchNo);

    /**
     * 原子认领一批待处理行（先回收超时 PROCESSING 行，再 UPDATE…RETURNING 认领置 PROCESSING）
     *
     * <p>认领由单条 SQL 原子完成，并发调用（含多实例）不会拿到同一批行。</p>
     *
     * @param limit 单次认领上限，建议 500
     * @return 本轮认领成功的行列表（status 均为 PROCESSING）；空列表表示本轮无待处理
     */
    List<StgStaffRecordDO> claimPendingBatch(int limit);

    /**
     * 单条标记 PROCESSED（处理完立即调用）
     *
     * @param id             行主键
     * @param processBatchNo 处理批次号
     */
    void markOneProcessed(Long id, String processBatchNo);
}
