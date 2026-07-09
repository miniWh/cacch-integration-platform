package com.cacch.integration.entity.meeting;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.integration.dao.typehandler.PostgreSqlJsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会议纪要 DO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_meeting_minutes", autoResultMap = true)
public class MeetingMinutesDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联会议记录主键
     */
    private Long meetingId;

    /**
     * 纪要原文（腾讯智能纪要文本）
     */
    private String rawContent;

    /**
     * 纪要摘要（可选）
     */
    private String summary;

    /**
     * 关键词列表（JSONB）
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<String> keywords;

    /**
     * 发言人摘要列表（JSONB）
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<Map<String, String>> speakerSummary;

    /**
     * 解析出的待办列表（JSONB，元素含 title 等字段）
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<Map<String, Object>> todoList;

    /**
     * 拉取/解析状态，见 {@link com.cacch.integration.common.enums.meeting.MinutesFetchStatusEnum}
     */
    private Integer status;

    /**
     * 最近一次成功拉取纪要的时间
     */
    private LocalDateTime fetchTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
