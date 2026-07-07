package com.cacch.integration.manager.meeting.api;

import com.cacch.integration.entity.meeting.MeetingRecordDO;

/**
 * 会议纪要拉取与待办解析编排接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMeetingMinutesManager {

    /**
     * 尝试拉取已结束会议的 TXT 智能纪要并解析待办入库
     *
     * @param record 会议记录（须含 wecomMeetingId）
     * @return 1 已处理，0 跳过，-1 失败
     */
    int trySyncMinutes(MeetingRecordDO record);
}
