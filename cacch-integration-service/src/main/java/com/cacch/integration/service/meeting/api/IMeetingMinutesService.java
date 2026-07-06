package com.cacch.integration.service.meeting.api;

import com.cacch.integration.entity.meeting.MeetingMinutesDO;

/**
 * 会议纪要服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMeetingMinutesService {

    /**
     * 按会议 ID 查询会议纪要
     *
     * @param meetingId 会议记录主键
     * @return 会议纪要，不存在时返回 null
     */
    MeetingMinutesDO getByMeetingId(Long meetingId);

    /**
     * 新增会议纪要
     *
     * @param minutes 待保存实体
     */
    void save(MeetingMinutesDO minutes);

    /**
     * 按主键更新会议纪要
     *
     * @param minutes 待更新实体（须含 id）
     */
    void updateById(MeetingMinutesDO minutes);
}
