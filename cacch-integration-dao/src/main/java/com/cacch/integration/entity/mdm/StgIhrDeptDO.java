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
 * iHR 部门信息贴源 DO —— 映射 PG 表 {@code stg_ihr_dept}
 *
 * <p>贴源层：按批次保留 iHR「部门查询」接口的原始响应报文，
 * 业务键为 {@code (etl_batch_no, ihr_dept_uuid)}（UNIQUE 约束）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("stg_ihr_dept")
public class StgIhrDeptDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * ETL 批次号（每轮拉取生成，DEPT + yyyyMMddHHmmssSSS）
     */
    private String etlBatchNo;

    /**
     * iHR 部门 UUID
     */
    private String ihrDeptUuid;

    /**
     * iHR 原始响应报文
     */
    private String rawJson;

    /**
     * 拉取时 HTTP 状态码
     */
    private Integer httpStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
