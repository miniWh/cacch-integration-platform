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
 * 人员主数据变更事件 DO —— 映射 PG 表 {@code sys_event}
 *
 * <p>事件层：落库过程中识别的入离职/调岗/属性更新事件，
 * {@code eventId} 为幂等键（UNIQUE），重复产生时 DO NOTHING 吞掉。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("sys_event")
public class SysEventDO {

    /**
     * 内部主键（雪花生成；手写 insertIgnore SQL 不触发 ASSIGN_ID，由 Service 层预生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 事件幂等键 SHA-256(mdCode:eventType:rowHash)（UNIQUE）
     */
    private String eventId;

    /**
     * 事件类型：HIRE / LEAVE / REHIRE / TRANSFER / UPDATE
     */
    private String eventType;

    /**
     * 变更级别：HIGH-入离职 / NORMAL-调岗与属性更新
     */
    private String changeLevel;

    /**
     * 人员主数据编码
     */
    private Long mdCode;

    /**
     * 事件发生时工号（快照）
     */
    private String empNo;

    /**
     * 事件明细 JSON（任职、部门、职位、日期、批次号等；JSONB）
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
