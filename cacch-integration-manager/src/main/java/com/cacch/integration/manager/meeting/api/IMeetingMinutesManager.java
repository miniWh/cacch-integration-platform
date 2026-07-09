package com.cacch.integration.manager.meeting.api;

import com.cacch.integration.entity.meeting.MeetingRecordDO;

/**
 * 会议纪要拉取与待办解析编排接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMeetingMinutesManager {

    /**
     * 尝试拉取腾讯会议智能纪要并解析待办入库。
     *
     * <p>弱门槛：会议已开始（含可选宽限）后才查询；真正触发以录制/纪要就绪为准，
     * 不再等待计划结束时间，以支持提前结束的会议。</p>
     *
     * @param record 会议记录（须含会议号或腾讯 meeting_id）
     * @return 1 已处理，0 跳过，-1 失败
     */
    int trySyncMinutes(MeetingRecordDO record);
}
