package com.cacch.integration.entity.moka;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Moka 部门多语言名称子表 DO —— 映射 PG 表 {@code moka_department_localized}
 *
 * <p>复合主键：(department_code, locale)。MyBatis-Plus 不直接支持多主键查询，
 * 需在 Mapper 中自定义方法，或通过 {@link com.baomidou.mybatisplus.core.conditions.Wrapper} 组合条件。</p>
 *
 * <p>外键 {@code department_code} 级联关联主表，删除主表记录时子表记录自动清除。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("moka_department_localized")
public class MokaDepartmentLocalizedDO {

    /**
     * 关联部门 ID（主键之一 + 外键）
     */
    @TableField("department_code")
    private String departmentCode;

    /**
     * 语言代码（如 zh_CN / en_US），主键之二
     */
    @TableField("locale")
    private String locale;

    /**
     * 该语言对应的部门名称
     */
    @TableField("prop_value")
    private String propValue;
}
