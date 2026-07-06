package com.cacch.integration.entity.meeting;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.integration.dao.typehandler.PostgreSqlJsonbTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 会议记录 DO
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_meeting_record", autoResultMap = true)
public class MeetingRecordDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long smartTableId;

    private String recordId;

    private String meetingTitle;

    private String meetingDescription;

    private LocalDate meetingDate;

    private LocalTime startTime;

    private Integer duration;

    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private List<String> attendees;

    private String meetingLink;

    private String wecomMeetingId;

    private String wecomMeetingCode;

    private String status;

    private String minutesStatus;

    private Integer syncVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
