package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * iHR 增量员工暂存队列 DO —— 映射 PG 表 {@code stg_staff_record}
 *
 * <p>两阶段模式的中间表：Pull 任务把新增/更新员工 ID 入队，
 * Process 任务捞取后调 MdmEmpIngestServiceImpl.ingestOne 落库。</p>
 *
 * <p>唯一约束 {@code (staff_id, pull_batch_no)} 保证同一批次内不重复入队；
 * 冲突时 DO UPDATE 保留 ADD 优先级、lastUpdate 取较大值。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("stg_staff_record")
public class StgStaffRecordDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * iHR 员工 UUID
     */
    private String staffId;

    /**
     * 来源：ADD-新增接口 / UPDATE-更新接口
     */
    private String source;

    /**
     * 更新接口 V2 返回的秒级时间戳（新增接口固定填 0；Process 处理时按此字段升序）
     */
    private Long lastUpdate;

    /**
     * 拉取批次号
     */
    private String pullBatchNo;

    /**
     * 处理批次号
     */
    private String processBatchNo;

    /**
     * 处理状态：PENDING / PROCESSING / PROCESSED
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
