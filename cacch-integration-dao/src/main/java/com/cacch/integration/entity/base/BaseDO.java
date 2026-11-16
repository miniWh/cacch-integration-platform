package com.cacch.integration.entity.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 数据库实体基类 —— 承载全表标准公共字段
 *
 * <p>所有遵循标准字段约定的 DO 继承本类；继承字段上的 MyBatis-Plus 注解
 * （雪花主键、自动填充、逻辑删除）对子类同样生效。子类使用 {@code @Data} 时
 * 须配套 {@code @EqualsAndHashCode(callSuper = true)}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Setter
public abstract class BaseDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间（插入自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间（插入与更新自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记（0 正常 / 1 删除）
     */
    @TableLogic
    private Integer isDeleted;
}
