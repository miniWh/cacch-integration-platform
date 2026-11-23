package com.cacch.integration.common.constant.moka;

/**
 * Moka 待入职同步 —— 面试登记表 customField 字段 ID 与 section 常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MokaEntryCustomFieldConstants {

    private MokaEntryCustomFieldConstants() {
    }

    /**
     * 紧急联系人姓名
     */
    public static final int CUSTOM_FIELD_ID_EMERGENCY_NAME = 399165;

    /**
     * 紧急联系人关系
     */
    public static final int CUSTOM_FIELD_ID_EMERGENCY_RELATION = 412655;

    /**
     * 紧急联系人电话
     */
    public static final int CUSTOM_FIELD_ID_EMERGENCY_PHONE = 410633;

    /**
     * 是否全日制（多实例，educationInfo）
     */
    public static final int CUSTOM_FIELD_ID_IS_FULL_TIME = 399166;

    public static final String SECTION_BASIC_INFO = "basicInfo";

    public static final String SECTION_EDUCATION_INFO = "educationInfo";

    /**
     * 是否全日制落库值
     */
    public static final String FULL_TIME_YES = "是";

    public static final String FULL_TIME_NO = "否";
}