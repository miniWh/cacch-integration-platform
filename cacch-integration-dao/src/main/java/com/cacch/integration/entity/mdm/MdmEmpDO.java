package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人员主数据身份主档 DO —— 映射 PG 表 {@code mdm_emp}
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("mdm_emp")
public class MdmEmpDO {

    /**
     * 内部主键（雪花生成，BaseMapper.insert 自动填充 ASSIGN_ID）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
