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
 * 岗位主数据变更事件 DO —— 映射 PG 表 {@code mdm_position_event}
 *
 * <p>事件层：落库过程中识别的新建/更新/停用事件，
 * {@code eventId} 为幂等键（UNIQUE），重复产生时 DO NOTHING 吞掉。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("mdm_position_event")
public class MdmPositionEventDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 事件幂等键 SHA-256(positionUuid:eventType:rowHash)（UNIQUE）
     */
    private String eventId;

    /**
     * 事件类型：CREATE / UPDATE / DISABLE
     */
    private String eventType;

    /**
     * 变更级别：HIGH-新建/停用 / NORMAL-更新
     */
    private String changeLevel;

    /**
     * 关联 mdm_position.id
     */
    private Long positionId;

    /**
     * 事件发生时职位编号（快照）
     */
    private String positionCode;

    /**
     * 事件明细 JSON（positionId/positionUuid/positionCode/positionName/etlBatchNo；JSONB）
     */
    private String payload;

    /**
     * 产生本事件的 ETL 批次号
     */
    private String etlBatchNo;

    /**
     * 处理状态：PENDING-待消费
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
