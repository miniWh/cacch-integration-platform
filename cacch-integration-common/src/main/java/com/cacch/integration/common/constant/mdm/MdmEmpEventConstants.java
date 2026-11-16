package com.cacch.integration.common.constant.mdm;

/**
 * 人员主数据（MDM）人员子域 —— mdm_emp_event 人员特有事件常量
 *
 * <p>仅承载人员子域特有的事件类型；CREATE / UPDATE / DISABLE、变更级别、
 * 处理状态等组织实体通用字典见 {@link MdmEventConstants}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmEmpEventConstants {

    private MdmEmpEventConstants() {
    }

    // ==================== 人员特有事件类型（mdm_emp_event.event_type） ====================

    /**
     * 入职（首次建档）
     */
    public static final String EVENT_TYPE_HIRE = "HIRE";
    /**
     * 离职
     */
    public static final String EVENT_TYPE_LEAVE = "LEAVE";
    /**
     * 再入职（已建档人员新开任职）
     */
    public static final String EVENT_TYPE_REHIRE = "REHIRE";
    /**
     * 调岗（部门或职位变更）
     */
    public static final String EVENT_TYPE_TRANSFER = "TRANSFER";
}
