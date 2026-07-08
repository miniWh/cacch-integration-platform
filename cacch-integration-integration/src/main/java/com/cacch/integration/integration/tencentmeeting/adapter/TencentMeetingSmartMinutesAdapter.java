package com.cacch.integration.integration.tencentmeeting.adapter;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 腾讯会议智能纪要文本解析适配器
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
public final class TencentMeetingSmartMinutesAdapter {

    private TencentMeetingSmartMinutesAdapter() {
    }

    /**
     * 提取纪要摘要正文
     *
     * @param response 智能纪要响应
     * @return 摘要文本，无数据时返回 null
     */
    public static String resolveMinuteText(TencentMeetingSmartMinutesResponse response) {
        if (response == null || response.getMeetingMinute() == null) {
            log.warn("【TencentMeeting】智能纪要响应为空");
            return null;
        }
        String minute = response.getMeetingMinute().getMinute();
        return StringUtils.hasText(minute) ? minute.trim() : null;
    }

    /**
     * 提取用于待办解析的文本（优先 2todo，元宝纪要时 fallback 到 minute）
     *
     * @param response 智能纪要响应
     * @return 待办解析源文本，无数据时返回 null
     */
    public static String resolveTodoSourceText(TencentMeetingSmartMinutesResponse response) {
        if (response == null || response.getMeetingMinute() == null) {
            log.warn("【TencentMeeting】智能纪要响应为空");
            return null;
        }
        TencentMeetingSmartMinutesResponse.MeetingMinute meetingMinute = response.getMeetingMinute();
        if (StringUtils.hasText(meetingMinute.getTodo())) {
            log.info("【TencentMeeting】待办文本: {}", meetingMinute.getTodo());
            return meetingMinute.getTodo().trim();
        }
        if (StringUtils.hasText(meetingMinute.getMinute())) {
            log.info("【TencentMeeting】纪要摘要正文: {}", meetingMinute.getMinute());
            return meetingMinute.getMinute().trim();
        }
        return null;
    }
}
