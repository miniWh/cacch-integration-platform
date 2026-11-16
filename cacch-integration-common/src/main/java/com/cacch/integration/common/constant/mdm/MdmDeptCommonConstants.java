package com.cacch.integration.common.constant.mdm;

/**
 * 部门主数据（MDM）部门子域 —— 部门特有业务参数常量
 *
 * <p>仅承载部门子域特有的字典值；来源系统编码、批次号工具等跨子域公共内容
 * 见 {@link MdmCommonConstants}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmDeptCommonConstants {

    private MdmDeptCommonConstants() {
    }

    /**
     * 源系统实体类型：部门（src_ref 映射用）
     */
    public static final String SRC_ENTITY_TYPE_DEPT = "DEPT";

    /**
     * 部门停用状态
     */
    public static final String DEPT_STATUS_DISABLED = "DISABLE";
}
