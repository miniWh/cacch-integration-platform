package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门主数据变更事件 DO —— 映射 PG 表 {@code mdm_dept_event}
 *
 * <p>事件层：落库过程中识别的新建/更新/停用事件，
 * {@code eventId} 为幂等键（UNIQUE），重复产生时 DO NOTHING 吞掉；
 * 事件公共结构继承自 {@link MdmEventBaseDO}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_dept_event")
public class MdmDeptEventDO extends MdmEventBaseDO {

    /**
     * 关联 mdm_dept.id
     */
    private Long deptId;

    /**
     * 事件发生时部门编码（快照）
     */
    private String deptCode;
}
