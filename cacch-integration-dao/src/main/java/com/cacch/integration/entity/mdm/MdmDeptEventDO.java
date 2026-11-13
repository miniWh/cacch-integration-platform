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
 * 部门主数据变更事件 DO —— 映射 PG 表 {@code mdm_dept_event}
 *
 * <p>事件层：落库过程中识别的新建/更新/停用事件，
 * {@code eventId} 为幂等键（UNIQUE），重复产生时 DO NOTHING 吞掉。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("mdm_dept_event")
public class MdmDeptEventDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 事件幂等键 SHA-256(deptUuid:eventType:rowHash)（UNIQUE）
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
     * 关联 mdm_dept.id
     */
    private Long deptId;

    /**
     * 事件发生时部门编码（快照）
     */
    private String deptCode;

    /**
     * 事件明细 JSON（deptId/deptUuid/deptCode/deptName/etlBatchNo；JSONB）
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
