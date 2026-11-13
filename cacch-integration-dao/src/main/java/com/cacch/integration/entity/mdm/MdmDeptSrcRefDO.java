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
 * 部门主数据多源映射表 DO —— 映射 PG 表 {@code mdm_dept_src_ref}
 *
 * <p>iHR 等来源系统的部门标识（如 iHR 部门 uuid）与 mdm_dept.id 的映射，
 * 主档本体不存来源标识，多源追溯与融合经本表进行。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("mdm_dept_src_ref")
public class MdmDeptSrcRefDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联 mdm_dept.id
     */
    private Long deptId;

    /**
     * 来源系统编码：IHR / WECOM / OA ...
     */
    private String srcSysCode;

    /**
     * 源系统实体类型：DEPT
     */
    private String srcEntityType;

    /**
     * 源系统实体 ID（源系统内唯一 handle，如 iHR 部门 uuid）
     */
    private String srcEntityId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
