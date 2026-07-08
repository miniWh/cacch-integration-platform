package com.cacch.integration.manager.tencentmeeting.api.impl;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;
import com.cacch.integration.manager.tencentmeeting.api.ITencentMeetingManager;
import com.cacch.integration.service.tencentmeeting.api.ITencentMeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 腾讯会议编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
@RequiredArgsConstructor
public class TencentMeetingManagerImpl implements ITencentMeetingManager {

    private final ITencentMeetingService tencentMeetingService;

    @Override
    public TencentMeetingSmartMinutesResponse getSmartMinutes(String recordFileId, String operatorId) {
        return tencentMeetingService.getSmartMinutes(recordFileId, operatorId);
    }
}
