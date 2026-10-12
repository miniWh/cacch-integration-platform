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
 * Moka 待入职 —— iHR 同步状态表 DO
 *
 * <p>映射 PG 表 {@code t_integration_moka_entry_sync}。
 * 一条 Moka 待入职记录对应一条状态记录，通过 {@code application_id} 关联主表。
 * 当前仅标注 iHR 同步状态；后期接入其他系统时新增列。</p>
 *
 * <p>{@code ihr_sync_status} 取值见
 * {@link com.cacch.integration.common.enums.moka.MokaEntrySyncStatusEnum}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_entry_sync")
public class MokaEntrySyncDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Moka 申请 ID（UNIQUE，关联主表）
     */
    private Integer applicationId;

    /**
     * iHR 同步状态：PENDING / SUCCESS / FAILED / SKIPPED
     */
    private String ihrSyncStatus;

    /**
     * iHR 返回的 entryId（同步成功时回填）
     */
    private String ihrEntryId;

    /**
     * 最近一次同步结果/失败原因
     */
    private String ihrSyncMessage;

    /**
     * 最近一次 iHR 同步时间
     */
    private LocalDateTime ihrSyncTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
