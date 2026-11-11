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
 * iHR 员工信息规范化 DO —— 映射 PG 表 {@code ods_ihr_emp}
 *
 * <p>规范化层：字段清洗后的 iHR 员工单条最新快照，
 * 业务主键为 {@code ihr_staff_id}（UNIQUE 约束），
 * {@code rowHash} 用于行级幂等短路，{@code verNo} 每次内容变更递增。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("ods_ihr_emp")
public class OdsIhrEmpDO {

    /**
     * 内部主键（雪花生成；手写 upsert SQL 不触发 ASSIGN_ID，由 Service 层预生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * iHR 员工 ID（业务主键，UNIQUE）
     */
    private String ihrStaffId;

    /**
     * 工号
     */
    private String ihrStaffNo;

    /**
     * 姓名
     */
    private String ihrStaffName;

    /**
     * 手机号
     */
    private String ihrMobile;

    /**
     * 个人邮箱
     */
    private String ihrEmail;

    /**
     * 工作邮箱
     */
    private String ihrWorkEmail;

    /**
     * 证件类型：IDENTITY_CARD / CHINA_PASSPORT 等
     */
    private String ihrIdCardType;

    /**
     * 证件号码（用于 md_emp.id_hash 溯源；禁止打印到日志）
     */
    private String ihrIdCard;

    /**
     * 部门 ID（iHR 返回 Long，存字符串形式）
     */
    private String ihrDeptId;

    /**
     * 部门名称
     */
    private String ihrDeptName;

    /**
     * 职位 ID
     */
    private String ihrPositionId;

    /**
     * 职位名称
     */
    private String ihrPositionName;

    /**
     * 员工状态：IN_SERVICE-在职 / QUIT-离职
     */
    private String ihrStatus;

    /**
     * 性别：MALE / FEMALE
     */
    private String ihrGender;

    /**
     * 最高学历
     */
    private String ihrEducation;

    /**
     * 出生日期
     */
    private LocalDate ihrBirthday;

    /**
     * 入职日期
     */
    private LocalDate ihrHireDate;

    /**
     * 离职日期
     */
    private LocalDate ihrLeaveDate;

    /**
     * 试用结束日期
     */
    private LocalDate ihrProbationEndDate;

    /**
     * 变更检测哈希（SHA-256；与库中值一致时幂等短路）
     */
    private String rowHash;

    /**
     * 数据版本号（首次 1，每次内容变更 +1）
     */
    private Integer verNo;

    /**
     * 数据质量标记：0-正常 1-存在质量问题（如离职无离职日期）
     */
    private Integer dqFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
