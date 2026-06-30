package com.cacch.integration.dto.meeting.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 会议记录 VO
 */
@Data
public class MeetingRecordVO {

    private Long id;

    private Long smartTableId;

    private String recordId;

    private String meetingTitle;

    private LocalDate meetingDate;

    private LocalTime startTime;

    private Integer duration;

    private List<String> attendees;

    private String meetingLink;

    private String wecomMeetingId;

    private String wecomMeetingCode;

    private String status;

    private String minutesStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
