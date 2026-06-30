package com.cacch.integration.service.meeting.api;

import com.cacch.integration.entity.meeting.MeetingRecordDO;

import java.util.List;

/**
 * 会议记录服务
 */
public interface IMeetingRecordService {

    MeetingRecordDO getById(Long id);

    MeetingRecordDO getBySmartTableIdAndRecordId(Long smartTableId, String recordId);

    List<MeetingRecordDO> listBySmartTableId(Long smartTableId);

    List<MeetingRecordDO> listByStatus(String status);

    void save(MeetingRecordDO record);

    void updateById(MeetingRecordDO record);
}
