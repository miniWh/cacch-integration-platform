package com.cacch.integration.common.constant.mdm;

/**
 * 岗位主数据（MDM）岗位子域 —— 岗位特有业务参数常量
 *
 * <p>仅承载岗位子域特有的字典值；来源系统编码、批次号工具等跨子域公共内容
 * 见 {@link MdmCommonConstants}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmPositionCommonConstants {

    private MdmPositionCommonConstants() {
    }

    /**
     * 源系统实体类型：岗位（src_ref 映射用）
     */
    public static final String SRC_ENTITY_TYPE_POSITION = "POSITION";

    /**
     * 岗位停用状态
     */
    public static final String POSITION_STATUS_DISABLED = "DISABLE";
}
