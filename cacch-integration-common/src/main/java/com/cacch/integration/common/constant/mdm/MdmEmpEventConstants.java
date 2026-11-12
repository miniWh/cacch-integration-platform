package com.cacch.integration.common.constant.mdm;

/**
 * 人员主数据（MDM）人员子域 —— mdm_emp_event 事件域常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmEmpEventConstants {

    private MdmEmpEventConstants() {
    }

    // ==================== 事件类型（mdm_emp_event.event_type） ====================

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
    /**
     * 属性更新（部门职位未变的其余内容变更）
     */
    public static final String EVENT_TYPE_UPDATE = "UPDATE";

    // ==================== 变更级别（mdm_emp_event.change_level） ====================

    /**
     * 高级别变更：入职 / 离职 / 再入职
     */
    public static final String CHANGE_LEVEL_HIGH = "HIGH";
    /**
     * 普通级别变更：调岗 / 属性更新
     */
    public static final String CHANGE_LEVEL_NORMAL = "NORMAL";

    // ==================== 事件处理状态（mdm_emp_event.status） ====================

    /**
     * 待消费（初始状态）
     */
    public static final String EVENT_STATUS_PENDING = "PENDING";
}
