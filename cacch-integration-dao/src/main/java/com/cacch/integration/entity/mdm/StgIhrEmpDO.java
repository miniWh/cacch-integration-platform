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
 * iHR 员工信息贴源 DO —— 映射 PG 表 {@code stg_ihr_emp}
 *
 * <p>贴源层：按批次保留 iHR「获取员工基本信息」接口的原始响应报文，
 * 业务键为 {@code (etl_batch_no, ihr_staff_id)}（UNIQUE 约束）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("stg_ihr_emp")
public class StgIhrEmpDO {

    /**
     * 内部主键（雪花生成；手写 upsert SQL 不触发 ASSIGN_ID，由 Service 层预生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * ETL 批次号（每轮拉取生成，IHR + yyyyMMddHHmmssSSS）
     */
    private String etlBatchNo;

    /**
     * iHR 员工 ID（UUID）
     */
    private String ihrStaffId;

    /**
     * iHR 原始响应报文（JSONB 贴源留底）
     */
    private String rawJson;

    /**
     * 拉取时 HTTP 状态码（成功路径恒 200）
     */
    private Integer httpStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
