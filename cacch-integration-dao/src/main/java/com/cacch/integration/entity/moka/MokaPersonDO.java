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
 * Moka 人员信息 DO —— 映射 PG 表 {@code t_integration_moka_person}
 *
 * <p>数据来源链路：persondetail（外部表）→ ihr_department（关联 department_code）
 * → 本中间表。接口 1 负责同步落库，接口 2 负责将本中间表推送 Moka 开放平台。</p>
 *
 * <p>主键策略：雪花 BIGINT（{@link IdType#ASSIGN_ID}），
 * 业务唯一键 {@code user_id}（persondetail.userId）单独建 UNIQUE 约束。</p>
 *
 * <p>role_id 两阶段策略：
 * <ul>
 *     <li>阶段一：DDL DEFAULT 223379（Moka 默认角色），同步时不显式赋值</li>
 *     <li>阶段二：拉取角色后按 jobTitle 匹配 t_integration_moka_role 更新</li>
 * </ul>
 * </p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_person")
public class MokaPersonDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * iHR 员工 ID（persondetail.userId，业务唯一键，UNIQUE）
     */
    private String userId;

    /**
     * 工号（Moka API number）
     */
    private String employeeNo;

    /**
     * 姓名（Moka API name）
     */
    private String userName;

    /**
     * 昵称/花名（Moka API nickname，暂与 userName 同值）
     */
    private String nickname;

    /**
     * 工作邮箱（Moka API email，允许为空）
     */
    private String companyEmail;

    /**
     * 工作电话（Moka API phone）
     */
    private String contactPhone;

    /**
     * Moka 自定义角色 ID（Moka API roleId）
     *
     * <p>阶段一默认值 223379；阶段二按 jobTitle 匹配 t_integration_moka_role 后更新</p>
     */
    private Integer roleId;

    /**
     * 部门编号（关联 ihr_department.ihrDeptId 得到 departmentCode）
     */
    private String departmentCode;

    /**
     * 直属领导邮箱（从 persondetail.superiorsInfo 提取，映射 Moka API superiorEmail）
     */
    private String superiorEmail;

    /**
     * 员工状态（iHR 原始值）
     */
    private String employeeStatus;

    /**
     * Moka API deactivated：0-不禁用 1-禁用
     *
     * <p>注意：首次创建用户传 1 则 Moka 侧不会创建成功</p>
     */
    private Integer deactivated;

    /**
     * Moka 用户语言（DEFAULT 'zh-CN'）
     */
    private String locale;

    /**
     * Moka 用户时区（DEFAULT 'Asia/Shanghai'）
     */
    private String timezone;

    /**
     * Moka 开放平台同步状态：0-PENDING 1-SYNCED 2-SYNC_FAILED
     */
    private Integer mokaSyncStatus;

    /**
     * 最近一次推送 Moka 的时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 最近一次推送结果摘要
     */
    private String lastSyncResult;

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
}
