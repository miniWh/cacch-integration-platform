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
 * 人员主数据多源映射表 DO —— 映射 PG 表 {@code mdm_emp_src_ref}
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("mdm_emp_src_ref")
public class MdmEmpSrcRefDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联 mdm_emp.id
     */
    private Long empId;

    /**
     * 来源系统编码：IHR / WECOM / OA ...
     */
    private String srcSysCode;

    /**
     * 源系统实体类型：STAFF / USER / ACCOUNT
     */
    private String srcEntityType;

    /**
     * 源系统实体 ID（源系统内唯一 handle，如 iHR staffId UUID / 企微 userId）
     */
    private String srcEntityId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
