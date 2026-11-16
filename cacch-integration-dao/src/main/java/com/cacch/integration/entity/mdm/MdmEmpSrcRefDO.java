package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员主数据多源映射表 DO —— 映射 PG 表 {@code mdm_emp_src_ref}
 *
 * <p>多源映射公共结构继承自 {@link MdmSrcRefBaseDO}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_emp_src_ref")
public class MdmEmpSrcRefDO extends MdmSrcRefBaseDO {

    /**
     * 关联 mdm_emp.id
     */
    private Long empId;
}
