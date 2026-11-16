package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.integration.entity.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 人员主数据身份主档 DO —— 映射 PG 表 {@code mdm_emp}
 *
 * <p>标准公共字段（id/createdAt/updatedAt/isDeleted）继承自 {@link BaseDO}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_emp")
public class MdmEmpDO extends BaseDO {

    /**
     * 工号（变更不断链，仅覆盖更新）
     */
    private String empNo;

    /**
     * 姓名
     */
    private String empName;

    /**
     * 性别：MALE / FEMALE
     */
    private String gender;

    /**
     * 出生日期
     */
    private LocalDate birthday;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 个人邮箱
     */
    private String personalEmail;

    /**
     * 工作邮箱
     */
    private String workEmail;

    /**
     * 最高学历
     */
    private String education;

    /**
     * 证件类型：IDENTITY_CARD / CHINA_PASSPORT / FOREIGN_PASSPORT / MTP / MTPFHKAMR / OTHER
     */
    private String idCardType;

    /**
     * 证件号
     */
    private String idCard;

    /**
     * 首次入职日期
     */
    private LocalDate firstHireDate;

    /**
     * 当前离职日期快照
     */
    private LocalDate leaveDate;

    /**
     * 当前试用结束日期快照
     */
    private LocalDate probationEndDate;

    /**
     * 首次建档来源系统：IHR / WECOM / OA ...
     */
    private String srcSysCode;
}
