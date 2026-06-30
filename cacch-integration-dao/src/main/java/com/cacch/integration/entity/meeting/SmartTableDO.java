package com.cacch.integration.entity.meeting;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 智能表格配置 DO — 总控表(MASTER)与员工会议表(MEETING)
 */
@Data
@TableName(value = "t_integration_smart_table", autoResultMap = true)
public class SmartTableDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tableType;

    private String userId;

    private String tableName;

    private String docId;

    private String docUrl;

    private String meetingSheetId;

    @TableField(typeHandler = Jackson3TypeHandler.class)
    private Map<String, String> meetingColumnMapping;

    private String todoSheetId;

    @TableField(typeHandler = Jackson3TypeHandler.class)
    private Map<String, String> todoColumnMapping;

    private Integer status;

    private LocalDateTime lastSyncTime;

    private String lastSyncError;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
