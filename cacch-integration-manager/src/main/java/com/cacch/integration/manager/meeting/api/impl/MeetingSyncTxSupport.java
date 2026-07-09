package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.common.enums.meeting.MeetingRecordStatusEnum;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.service.meeting.api.IMeetingRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会议同步数据库写操作支持（独立 Bean 保证事务生效；不含 HTTP）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MeetingSyncTxSupport {

    private final IMeetingRecordService meetingRecordService;

    /**
     * 企微建会成功后回写本地会议记录（仅 DB）
     *
     * @param record           会议记录，不可为空
     * @param wecomMeetingId   企微会议 ID，不可为空
     * @param wecomMeetingCode 企微会议号，可为空
     * @param meetingLink      会议链接，可为空
     */
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void markMeetingCreated(MeetingRecordDO record, String wecomMeetingId,
                                   String wecomMeetingCode, String meetingLink) {
        record.setWecomMeetingId(wecomMeetingId);
        record.setWecomMeetingCode(wecomMeetingCode);
        record.setMeetingLink(meetingLink);
        record.setStatus(MeetingRecordStatusEnum.SCHEDULED.getCode());
        meetingRecordService.updateById(record);
        log.info("【MeetingSync】本地会议记录已标记为已创建, recordId={}, meetingId={}",
                record.getRecordId(), wecomMeetingId);
    }

    /**
     * 反向同步后更新本地会议记录（仅 DB）
     *
     * @param record 已应用企微详情变更的会议记录，不可为空
     */
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void updateMeetingAfterReverseSync(MeetingRecordDO record) {
        meetingRecordService.updateById(record);
        log.info("【MeetingSync】反向同步本地记录已更新, recordId={}, meetingId={}",
                record.getRecordId(), record.getWecomMeetingId());
    }
}
