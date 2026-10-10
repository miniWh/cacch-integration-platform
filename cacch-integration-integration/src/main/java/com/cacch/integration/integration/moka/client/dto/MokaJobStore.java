package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 职位查询响应中的职位所属门店信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobStore {

    /**
     * 门店 id
     */
    private Integer id;

    /**
     * 门店编码
     */
    private String apiCode;

    /**
     * 门店名称
     */
    private String name;
}
