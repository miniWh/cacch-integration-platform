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
 * 人员主数据身份主档 DO —— 映射 PG 表 {@code md_emp}
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("md_emp")
public class MdEmpDO {

    /**
     * 内部主键（雪花生成，BaseMapper.insert 自动填充 ASSIGN_ID）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 人员主数据编码（雪花生成，对外稳定标识，UNIQUE）
     */
    private Long mdCode;

    /**
     * 身份哈希 SHA-256(盐 + 证件号)，自然人唯一识别（UNIQUE）
     */
    private String idHash;

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
     * 邮箱（优先工作邮箱）
     */
    private String email;

    /**
     * 最高学历
     */
    private String education;

    /**
     * 首次入职日期（首次建档时取 iHR enrollInDate）
     */
    private LocalDate firstHireDate;

    /**
     * 首次建档来源系统：IHR
     */
    private String srcSysCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
