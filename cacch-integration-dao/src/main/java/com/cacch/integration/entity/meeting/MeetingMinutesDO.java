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
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_meeting_minutes", autoResultMap = true)
public class MeetingMinutesDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long meetingId;

    private String rawContent;

    private String summary;

    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<String> keywords;

    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<Map<String, String>> speakerSummary;

    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<Map<String, Object>> todoList;

    private Integer status;

    private LocalDateTime fetchTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
