package com.cacch.integration.integration.wecom.client.dto.meeting;

import lombok.Data;

/**
 * 企微会议 — 内部参会成员
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComMeetingMemberInfo {

    private String userid;

    private Integer status;
}
