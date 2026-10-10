package com.cacch.integration.dto.moka.vo;

import lombok.Data;

import java.util.List;

/**
 * Moka 职位列表视图对象 —— 职位查询响应 data 节点的 VO 包装
 *
 * <p>与 integration 层 {@code MokaJobData} 字段一一对应，全量透传职位总数与职位列表。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobListVO {

    /**
     * 职位总数量
     */
    private Integer total;

    /**
     * 职位列表；无数据时可能为 null 或空列表
     */
    private List<MokaJobVO> jobs;
}
