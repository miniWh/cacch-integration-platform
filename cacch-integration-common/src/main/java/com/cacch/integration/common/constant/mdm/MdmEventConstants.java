package com.cacch.integration.common.constant.mdm;

/**
 * 主数据（MDM）域级公共事件常量 —— 人员 / 部门 / 岗位子域共用
 *
 * <p>承载各子域 {@code mdm_*_event} 表共享的事件类型、变更级别与处理状态字典；
 * 人员子域特有事件类型（HIRE / LEAVE / REHIRE / TRANSFER）保留在
 * {@link MdmEmpEventConstants} 中。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmEventConstants {

    private MdmEventConstants() {
    }

    // ==================== 事件类型（mdm_*_event.event_type，组织实体通用） ====================

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

    // ==================== 变更级别（mdm_*_event.change_level） ====================

    /**
     * 高级别变更：入职 / 离职 / 再入职 / 新建 / 停用
     */
    public static final String CHANGE_LEVEL_HIGH = "HIGH";
    /**
     * 普通级别变更：调岗 / 属性更新
     */
    public static final String CHANGE_LEVEL_NORMAL = "NORMAL";

    // ==================== 事件处理状态（mdm_*_event.status） ====================

    /**
     * 待消费（初始状态）
     */
    public static final String EVENT_STATUS_PENDING = "PENDING";
}
