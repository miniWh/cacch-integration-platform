package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 职位查询响应中的职级信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobRankInfo {

    /**
     * 职级 id
     */
    private Integer id;

    /**
     * 职级名称
     */
    private String name;

    /**
     * 职级别
     */
    private String type;

    /**
     * 职级顺序
     */
    private Integer level;

    /**
     * 职级备注
     */
    private String comment;
}
