package com.cacch.integration.common.constant.mdm;

/**
 * 人员主数据（MDM）任职域常量 —— 对应表 {@code mdm_emp_org_assign} 字典
 *
 * <p>承载任职状态枚举值与主任职标记常量。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmEmpOrgAssignConstants {

    private MdmEmpOrgAssignConstants() {
    }

    // ==================== 任职状态（mdm_emp_org_assign.assign_status） ====================

    /**
     * 待入职
     */
    public static final String ASSIGN_STATUS_PENDING_ONBOARD = "PENDING_ONBOARD";
    /**
     * 试用中
     */
    public static final String ASSIGN_STATUS_PROBATION = "PROBATION";
    /**
     * 正式
     */
    public static final String ASSIGN_STATUS_ACTIVE = "ACTIVE";
    /**
     * 已结束
     */
    public static final String ASSIGN_STATUS_ENDED = "ENDED";

    // ==================== 主任职标记（mdm_emp_org_assign.principal_flag） ====================

    /**
     * 主任职（当前单任职来源，恒为 Y）
     */
    public static final String PRINCIPAL_FLAG_YES = "Y";
}
