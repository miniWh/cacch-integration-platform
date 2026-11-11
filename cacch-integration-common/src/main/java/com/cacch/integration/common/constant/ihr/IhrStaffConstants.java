package com.cacch.integration.common.constant.ihr;

/**
 * IHR 员工域专有常量类
 *
 * <p>承载员工域各接口共享的请求参数枚举、默认值等业务参数。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class IhrStaffConstants {

    private IhrStaffConstants() {
    }

    // ==================== 员工状态 ====================

    /**
     * 在职
     */
    public static final String STAFF_STATUS_IN_SERVICE = "IN_SERVICE";
    /**
     * 离职
     */
    public static final String STAFF_STATUS_QUIT = "QUIT";

    // ==================== 证件类型 ====================

    /**
     * 居民身份证
     */
    public static final String ID_CARD_TYPE_IDENTITY_CARD = "IDENTITY_CARD";
    /**
     * 中国护照
     */
    public static final String ID_CARD_TYPE_CHINA_PASSPORT = "CHINA_PASSPORT";
    /**
     * 外国护照
     */
    public static final String ID_CARD_TYPE_FOREIGN_PASSPORT = "FOREIGN_PASSPORT";
    /**
     * 台湾居民来往大陆通行证
     */
    public static final String ID_CARD_TYPE_MTP = "MTP";
    /**
     * 港澳居民来往内地通行证
     */
    public static final String ID_CARD_TYPE_MTPFHKAMR = "MTPFHKAMR";
    /**
     * 其他
     */
    public static final String ID_CARD_TYPE_OTHER = "OTHER";

    // ==================== 性别 ====================

    /**
     * 男
     */
    public static final String SEX_MALE = "MALE";
    /**
     * 女
     */
    public static final String SEX_FEMALE = "FEMALE";

    // ==================== 员工类型 ====================

    /**
     * 全职
     */
    public static final String STAFF_TYPE_FULLTIME = "FULLTIME";
    /**
     * 兼职
     */
    public static final String STAFF_TYPE_PARTTIME = "PARTTIME";
    /**
     * 实习
     */
    public static final String STAFF_TYPE_INTERSHIP = "INTERSHIP";
    /**
     * 外派
     */
    public static final String STAFF_TYPE_EXPATRIATE = "EXPATRIATE";
    /**
     * 临时工
     */
    public static final String STAFF_TYPE_TEMPORARY = "TEMPORARY";
    /**
     * 退休返聘
     */
    public static final String STAFF_TYPE_REHIRE_RETIREMENT = "REHIRE_RETIREMENT";

    // ==================== 婚姻状态 ====================

    /**
     * 未婚
     */
    public static final String MARRY_STATUS_UNMARRIED = "UNMARRIED";
    /**
     * 已婚
     */
    public static final String MARRY_STATUS_MARRIED = "MARRIED";
    /**
     * 已婚已育
     */
    public static final String MARRY_STATUS_M_CHILDREN = "M_CHILDREN";

    // ==================== 最高学历 ====================

    /**
     * 博士
     */
    public static final String HIGHEST_EDUCATION_DOCTOR = "DOCTOR";
    /**
     * 硕士 / MBA / EMBA
     */
    public static final String HIGHEST_EDUCATION_MASTER = "MASTER";
    /**
     * 本科
     */
    public static final String HIGHEST_EDUCATION_UNDER_GRADUATE = "UNDER_GRADUATE";
    /**
     * 大专 / 高职
     */
    public static final String HIGHEST_EDUCATION_JUNIOR = "JUNIOR";
    /**
     * 高中 / 中专 / 中技
     */
    public static final String HIGHEST_EDUCATION_SENIOR_MIDDLE = "SENIOR_MIDDLE";
    /**
     * 初中
     */
    public static final String HIGHEST_EDUCATION_JUNIOR_MIDDLE = "JUNIOR_MIDDLE";
    /**
     * 小学
     */
    public static final String HIGHEST_EDUCATION_PRIMARY_SCHOOL = "PRIMARY_SCHOOL";
    /**
     * 其他
     */
    public static final String HIGHEST_EDUCATION_OTHER = "OTHER";

    // ==================== 默认值 ====================

    /**
     * 兜底页大小 —— IHR 接口文档未显式声明 pageSize 时使用
     */
    public static final int DEFAULT_PAGE_SIZE = 500;
}
