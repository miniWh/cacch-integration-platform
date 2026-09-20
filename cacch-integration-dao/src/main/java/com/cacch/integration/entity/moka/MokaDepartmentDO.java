package com.cacch.integration.entity.moka;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Moka 组织架构部门主表 DO —— 映射 PG 表 {@code moka_department}
 *
 * <p>主键为客户系统的 department_code（业务唯一标识），
 * 并非雪花 BIGINT 主键。MyBatis-Plus 需使用 {@link IdType#INPUT} 或 {@link IdType#NONE}。</p>
 *
 * <p>注意：该表与本项目其他业务表（{@code t_integration_*}）结构不同，
 * 缺少 {@code is_deleted} 逻辑删除字段和 {@code updated_at} 更新时间字段，
 * 按实际 DDL 对齐。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_department")
public class MokaDepartmentDO {

    /**
     * 客户系统的部门 id（主键，业务唯一标识）
     */
    @TableId(type = IdType.INPUT)
    private String departmentCode;

    /**
     * 部门名称（主语言）
     */
    private String name;

    /**
     * 上级部门唯一 id，一级部门传 "0"
     */
    private String parentCode;

    /**
     * 部门类型：1-普通部门（默认），2-门店部门
     */
    private Integer type;

    /**
     * 部门排序，支持 0~10000 两位小数，为空默认排在最后
     */
    private BigDecimal sequence;

    /**
     * 记录创建时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
