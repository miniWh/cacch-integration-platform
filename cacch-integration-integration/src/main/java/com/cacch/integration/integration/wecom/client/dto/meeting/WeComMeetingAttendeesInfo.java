package com.cacch.integration.integration.wecom.client.dto.meeting;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 企微会议 — 参会成员集合
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComMeetingAttendeesInfo {

    private List<WeComMeetingMemberInfo> member;

    /**
     * 提取内部成员 userid 列表（去重、保序）
     */
    public List<String> extractMemberUserIds() {
        if (member == null || member.isEmpty()) {
            return List.of();
        }
        List<String> userIds = new ArrayList<>();
        for (WeComMeetingMemberInfo item : member) {
            if (item.getUserid() != null && !item.getUserid().isBlank()
                    && !userIds.contains(item.getUserid())) {
                userIds.add(item.getUserid());
            }
        }
        return userIds;
    }
}
