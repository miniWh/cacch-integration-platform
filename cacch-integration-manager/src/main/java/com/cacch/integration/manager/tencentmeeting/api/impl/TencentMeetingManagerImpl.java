package com.cacch.integration.manager.tencentmeeting.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.tencentmeeting.adapter.TencentMeetingRecordFileResolver;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingRecordAddressesResponse;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;
import com.cacch.integration.manager.tencentmeeting.api.ITencentMeetingManager;
import com.cacch.integration.service.tencentmeeting.api.ITencentMeetingService;
import com.cacch.integration.service.tencentmeeting.api.ITencentMeetingUserMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 腾讯会议编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TencentMeetingManagerImpl implements ITencentMeetingManager {

    private final ITencentMeetingService tencentMeetingService;
    private final ITencentMeetingUserMappingService tencentMeetingUserMappingService;

    @Override
    public TencentMeetingSmartMinutesResponse getSmartMinutes(String recordFileId, String wecomOperatorId) {
        String txOperatorId = resolveTxMeetingUserId(wecomOperatorId);
        return tencentMeetingService.getSmartMinutes(recordFileId, txOperatorId);
    }

    @Override
    public String resolveTencentRecordFileId(String meetingRecordId, String wecomRecordFileId, int sessionIndex,
                                             String wecomOperatorId) {
        if (!StringUtils.hasText(meetingRecordId)) {
            log.info("【TencentMeeting】跳过 record_file_id 解析, meetingRecordId 为空, wecomRecordFileId={}",
                    wecomRecordFileId);
            return null;
        }
        String txOperatorId = resolveTxMeetingUserId(wecomOperatorId);
        TencentMeetingRecordAddressesResponse addresses =
                tencentMeetingService.listRecordAddresses(meetingRecordId, txOperatorId);
        String txRecordFileId = TencentMeetingRecordFileResolver.resolveTencentRecordFileId(
                addresses, wecomRecordFileId, sessionIndex);
        if (!StringUtils.hasText(txRecordFileId)) {
            log.info("【TencentMeeting】未解析到腾讯会议 record_file_id, meetingRecordId={}, wecomRecordFileId={}, "
                            + "sessionIndex={}",
                    meetingRecordId, wecomRecordFileId, sessionIndex);
            return null;
        }
        log.info("【TencentMeeting】record_file_id 映射, meetingRecordId={}, wecomRecordFileId={}, "
                        + "txRecordFileId={}, sessionIndex={}",
                meetingRecordId, wecomRecordFileId, txRecordFileId, sessionIndex);
        return txRecordFileId;
    }

    /**
     * 将企微 userid 映射为腾讯会议 userid（所有腾讯会议 API 传参统一走此方法）
     *
     * @param wecomUserId 企微 userid
     * @return 腾讯会议 userid
     */
    @Override
    public String resolveTxMeetingUserId(String wecomUserId) {
        if (!StringUtils.hasText(wecomUserId)) {
            throw new BizException(ResultCode.PARAM_INVALID, "企微 userid 为空，无法调用腾讯会议 API");
        }
        String normalizedWecomUserId = wecomUserId.trim();
        String txMeetingUserId = tencentMeetingUserMappingService.resolveTxMeetingUserId(normalizedWecomUserId);
        if (!StringUtils.hasText(txMeetingUserId)) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "未找到企微用户对应的腾讯会议 userid, wecomUserId=" + normalizedWecomUserId);
        }
        log.info("【TencentMeeting】用户ID映射, wecomUserId={}, txMeetingUserId={}",
                normalizedWecomUserId, txMeetingUserId);
        return txMeetingUserId;
    }
}
