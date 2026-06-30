package com.cacch.integration.service.meeting.api;

import com.cacch.integration.entity.meeting.MeetingMinutesDO;

/**
 * 会议纪要服务
 */
public interface IMeetingMinutesService {

    MeetingMinutesDO getByMeetingId(Long meetingId);

    void save(MeetingMinutesDO minutes);

    void updateById(MeetingMinutesDO minutes);
}
