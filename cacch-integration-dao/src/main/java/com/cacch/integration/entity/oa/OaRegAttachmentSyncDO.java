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
 * 国内登记报告附件同步记录 DO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_oa_reg_attachment_sync")
public class OaRegAttachmentSyncDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
     * 资料项目 field0214
     */
    private String itemName;

    /**
     * 子表行 formson_5464.id；OA 未匹配时可为空
     */
    private Long itemRowId;

    /**
     * 共享盘目录完整路径
     */
    private String sharePath;

    /**
     * 同步文件名
     */
    private String fileName;

    /**
     * 解析版本号
     * @deprecated 已改用「最终版本」后缀 + 文件创建时间，保留字段兼容历史数据
     */
    private Integer fileVersion;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件 SHA-256
     */
    private String fileChecksum;

    /**
     * 共享盘文件创建时间（幂等比对）
     */
    private LocalDateTime fileCreatedAt;

    /**
     * 共享盘文件修改时间
     */
    private LocalDateTime fileModifiedAt;

    /**
     * OA REST 上传返回 fileUrl
     */
    private String oaFileId;

    /**
     * CAP4 绑定 subReference（field0218）
     */
    private String oaSubReference;

    /**
     * 同步状态：PENDING/SUCCESS/RETRY/FAILED/SKIPPED
     */
    private String syncStatus;

    /**
     * 同步说明或失败原因
     */
    private String syncMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最近一次同步时间
     */
    private LocalDateTime lastSyncAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
