package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Moka ATS —— 职位查询响应 data 节点
 *
 * <p>包含职位总数量与职位列表，由 {@link MokaJobListResponse} 泛型引用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobData {

    /**
     * 职位总数量
     */
    private Integer total;

    /**
     * 职位列表；无数据时可能为 null 或空列表
     */
    private List<MokaJobItem> jobs;
}
