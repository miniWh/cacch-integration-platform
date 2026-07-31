package com.cacch.integration.entity.oa;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 共享盘目录治理记录 DO（append 模式：每轮执行新增记录，保留全部历史）
 *
 * <p>映射表 {@code t_integration_oa_reg_share_dir_provision}
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_oa_reg_share_dir_provision")
public class OaRegShareDirProvisionDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 执行轮次标识（时间戳+UUID），同轮所有记录共享
     */
    private String runId;

    /**
     * OA 主表 formmain_4070.id
     */
    private Long formMainId;

    /**
     * 登记负责人 field0223
     */
    private String ownerName;

    /**
     * IPDP 名称 field0160
     */
    private String ipdpName;

    /**
     * IPDP 项目编号 field0164
     */
    private String ipdpProjectNo;

    /**
     * 资料项目名称 field0214
     */
    private String itemName;

    /**
     * 子表行 formson_5464.id
     */
    private String itemRowId;

    /**
     * 需要/不需要快照（field0216 原始值：0=需要，1=不需要）
     */
    private String itemRequired;

    /**
     * L3 完整路径（归一化后）
     */
    private String sharePath;

    /**
     * 所属路径组 groupRetain 决策（true=保留/创建）
     */
    private Boolean groupRetain;

    /**
     * 治理动作：CREATED/DELETED/SKIPPED_EXISTS/SKIPPED_NOT_EMPTY/SKIPPED_NOT_REQUIRED/SKIPPED_GROUP_RETAINED/FAILED
     */
    private String action;

    /**
     * 跳过或失败原因
     */
    private String actionMessage;

    /**
     * 本次治理时间
     */
    private LocalDateTime provisionedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
