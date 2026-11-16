package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员主数据变更事件 DO —— 映射 PG 表 {@code mdm_emp_event}
 *
 * <p>事件层：落库过程中识别的入离职/调岗/属性更新事件，
 * {@code eventId} 为幂等键（UNIQUE），重复产生时 DO NOTHING 吞掉；
 * 事件公共结构继承自 {@link MdmEventBaseDO}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_emp_event")
public class MdmEmpEventDO extends MdmEventBaseDO {

    /**
     * 关联 mdm_emp.id
     */
    private Long empId;

    /**
     * 事件发生时工号（快照）
     */
    private String empNo;
}
