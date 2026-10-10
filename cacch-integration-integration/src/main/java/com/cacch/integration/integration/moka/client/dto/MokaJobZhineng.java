package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 职位查询响应中的职位职能信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobZhineng {

    /**
     * 职能 id
     */
    private Integer id;

    /**
     * 职能名称
     */
    private String name;
}
