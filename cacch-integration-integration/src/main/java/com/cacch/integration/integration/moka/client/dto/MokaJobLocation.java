package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 职位查询响应中的职位地点信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobLocation {

    /**
     * 地址 id
     */
    private Integer id;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 地区
     */
    private String area;

    /**
     * 地点所在国家
     */
    private String country;

    /**
     * 地点详细信息
     */
    private String address;
}
