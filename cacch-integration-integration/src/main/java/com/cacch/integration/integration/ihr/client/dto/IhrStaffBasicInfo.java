package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;

/**
 * IHR「获取员工基本信息」接口业务数据
 *
 * <p>所有字段直接映射 iHR 响应 data 对象的同名属性；
 * 日期字段均为 String（格式 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss），不做 LocalDate/LocalDateTime 转换。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrStaffBasicInfo {

    /**
     * 员工ID（UUID）
     */
    private String staffId;

    /**
     * 工号
     */
    private String staffNo;

    /**
     * 姓名
     */
    private String staffName;

    /**
     * 英文姓名
     */
    private String englishName;

    /**
     * 花名
     */
    private String nickName;

    /**
     * 手机号
     */
    private String mobileNo;

    /**
     * 电子地址（个人邮箱）
     */
    private String email;

    /**
     * 工作邮箱
     */
    private String workEmail;

    /**
     * 工作电话
     */
    private String workPhone;

    /**
     * 员工状态：IN_SERVICE（在职）/ QUIT（离职）
     */
    private String staffStatus;

    /**
     * 证件类型：IDENTITY_CARD / CHINA_PASSPORT / FOREIGN_PASSPORT / MTP / MTPFHKAMR / OTHER
     */
    private String idCardType;

    /**
     * 证件号码
     */
    private String idCardNo;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 职位ID
     */
    private String positionId;

    /**
     * 职位名称
     */
    private String positionName;

    /**
     * 性别：MALE / FEMALE
     */
    private String sex;

    /**
     * 头像ID
     */
    private String staffImageId;

    /**
     * 员工类型：FULLTIME / PARTTIME / INTERSHIP / EXPATRIATE / TEMPORARY / REHIRE_RETIREMENT
     */
    private String staffType;

    /**
     * 婚姻状态：UNMARRIED / MARRIED / M_CHILDREN
     */
    private String marryStatus;

    /**
     * 最高学历：DOCTOR / MASTER / UNDER_GRADUATE / JUNIOR / SENIOR_MIDDLE / JUNIOR_MIDDLE / PRIMARY_SCHOOL / OTHER
     */
    private String highestEducation;

    /**
     * 联系地址
     */
    private String workPlace;

    /**
     * 工作地点ID
     */
    private String companySiteId;

    /**
     * 工作地点名称
     */
    private String companySiteName;

    /**
     * 出生日期，格式 yyyy-MM-dd
     */
    private String birthday;

    /**
     * 合同开始日期，格式 yyyy-MM-dd
     */
    private String contractBeginDate;

    /**
     * 合同结束日期，格式 yyyy-MM-dd
     */
    private String contractEndDate;

    /**
     * 入职日期，格式 yyyy-MM-dd
     */
    private String enrollInDate;

    /**
     * 离职日期，格式 yyyy-MM-dd
     */
    private String leaveDate;

    /**
     * 试用期结束日期，格式 yyyy-MM-dd
     */
    private String probationEndDate;

    /**
     * 创建日期，格式 yyyy-MM-dd HH:mm:ss
     */
    private String createdDate;

    /**
     * 最后修改日期，格式 yyyy-MM-dd HH:mm:ss
     */
    private String lastUpdateDate;
}
