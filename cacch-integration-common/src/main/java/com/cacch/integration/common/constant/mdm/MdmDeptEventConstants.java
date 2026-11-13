package com.cacch.integration.common.constant.mdm;

/**
 * 部门主数据（MDM）部门子域 —— mdm_dept_event 事件域常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmDeptEventConstants {

    private MdmDeptEventConstants() {
    }

    // ==================== 事件类型（mdm_dept_event.event_type） ====================

    /**
     * 新建（首次建档）
     */
    public static final String EVENT_TYPE_CREATE = "CREATE";
    /**
     * 属性更新（已有主档内容变更）
     */
    public static final String EVENT_TYPE_UPDATE = "UPDATE";
    /**
     * 停用（全量同步比对发现 iHR 侧已不存在）
     */
    public static final String EVENT_TYPE_DISABLE = "DISABLE";

    // ==================== 变更级别（mdm_dept_event.change_level） ====================

    /**
     * 高级别变更：新建 / 停用
     */
    public static final String CHANGE_LEVEL_HIGH = "HIGH";
    /**
     * 普通级别变更：属性更新
     */
    public static final String CHANGE_LEVEL_NORMAL = "NORMAL";

    // ==================== 事件处理状态（mdm_dept_event.status） ====================

    /**
     * 待消费（初始状态）
     */
    public static final String EVENT_STATUS_PENDING = "PENDING";
}
