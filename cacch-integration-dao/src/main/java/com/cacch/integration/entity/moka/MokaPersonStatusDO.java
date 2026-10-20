package com.cacch.integration.entity.moka;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Moka 人员入离职状态 DO —— 映射 PG 表 {@code t_integration_moka_person_status}
 *
 * <p>数据来源链路：t_integration_moka_person（employee_no = persondetail.staffNo，
 * 一一对应且唯一）→ 本状态表 → Moka hired / resign 申请状态接口。</p>
 *
 * <p>主键策略：雪花 BIGINT（{@link IdType#ASSIGN_ID}），业务唯一键
 * {@code employee_no}（工号）单独建 UNIQUE 约束，一人一行，
 * 入职 / 离职同步状态互不影响（同一行分列记录，覆盖员工在职 → 离职的生命周期流转）。</p>
 *
 * <p>{@code applicationId}（Moka 申请 id）获取机制待定，当前允许为空；
 * 处理时为空则跳过该记录且不写状态行，机制确定后无需重置历史数据。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_person_status")
public class MokaPersonStatusDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 工号（t_integration_moka_person.employee_no = persondetail.staffNo，业务唯一键，UNIQUE）
     */
    private String employeeNo;

    /**
     * Moka 申请 id（获取机制待定，当前为空；为空时跳过同步且不写状态行）
     */
    private Integer applicationId;

    /**
     * 入职同步状态：0-PENDING 1-SYNCED 2-SYNC_FAILED
     */
    private Integer hiredSyncStatus;

    /**
     * 最近一次入职同步失败原因摘要
     */
    private String hiredError;

    /**
     * 最近一次入职同步时间
     */
    private LocalDateTime hiredSyncTime;

    /**
     * 离职同步状态：0-PENDING 1-SYNCED 2-SYNC_FAILED
     */
    private Integer resignSyncStatus;

    /**
     * 最近一次离职同步失败原因摘要
     */
    private String resignError;

    /**
     * 最近一次离职同步时间
     */
    private LocalDateTime resignSyncTime;

    /**
     * 记录创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-正常 1-删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * iHR 员工 ID（关联查询从 t_integration_moka_person.user_id 获取）
     */
    @TableField(exist = false)
    private String userId;

    /**
     * 姓名（关联查询从 t_integration_moka_person.user_name 获取）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 手机号（关联查询从 t_integration_moka_person.contact_phone 获取）
     */
    @TableField(exist = false)
    private String contactPhone;

    /**
     * 员工状态（关联查询从 t_integration_moka_person.employee_status 获取）
     */
    @TableField(exist = false)
    private String employeeStatus;
}
