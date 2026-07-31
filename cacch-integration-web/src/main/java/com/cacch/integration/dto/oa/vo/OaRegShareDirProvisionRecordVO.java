package com.cacch.integration.dto.oa.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 共享盘目录治理记录 VO（REQ-OA-002）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaRegShareDirProvisionRecordVO {

    private Long id;

    /**
     * 执行轮次标识
     */
    private String runId;

    /**
     * OA 主表 formmain_4070.id
     */
    private Long formMainId;

    /**
     * 登记负责人
     */
    private String ownerName;

    /**
     * IPDP 名称
     */
    private String ipdpName;

    /**
     * IPDP 项目编号
     */
    private String ipdpProjectNo;

    /**
     * 资料项目名称
     */
    private String itemName;

    /**
     * 子表行 ID
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
     * 路径组 groupRetain 决策（true=保留/创建）
     */
    private Boolean groupRetain;

    /**
     * 治理动作
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

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
