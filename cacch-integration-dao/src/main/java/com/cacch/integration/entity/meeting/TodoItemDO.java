package com.cacch.integration.entity.meeting;

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
 * 待办事项 DO
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_todo_item")
public class TodoItemDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long meetingId;

    private Long smartTableId;

    private String recordId;

    private String todoTitle;

    private String assignee;

    private String assigneeName;

    private LocalDate dueDate;

    private String priority;

    private String source;

    private String sourceText;

    private String status;

    private LocalDateTime completedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
