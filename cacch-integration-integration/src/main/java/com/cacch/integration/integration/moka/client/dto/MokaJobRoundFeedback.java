package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Moka ATS —— 职位查询响应中的面试评价表信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobRoundFeedback {

    /**
     * 面试轮次 id
     */
    private Long roundId;

    /**
     * 面试评价表 id
     */
    private String feedbackId;

    /**
     * 面试轮次关联的用户组 id 列表；无关联时可能为 null 或空列表
     */
    private List<Long> userGroupsIds;
}
