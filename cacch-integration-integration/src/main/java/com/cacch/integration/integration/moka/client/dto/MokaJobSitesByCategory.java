package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Moka ATS —— 职位查询响应中的官网发布分布（根据官网类型区分）
 *
 * <p>注意：{@code recommend_campus} / {@code recommend_social} 为 Moka 原始下划线字段名，
 * 通过 {@code @JsonProperty} 映射到 camelCase Java 字段。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobSitesByCategory {

    /**
     * 职位关联的社招官网 id 列表
     */
    private List<Long> social;

    /**
     * 职位关联的校招官网 id 列表
     */
    private List<Long> camp;

    /**
     * 职位关联的全局内推官网 id 列表
     */
    private List<Long> recommend;

    /**
     * 职位关联的校招内推官网 id 列表（Moka 原始字段名 recommend_campus）
     */
    @JsonProperty("recommend_campus")
    private List<Long> recommendCampus;

    /**
     * 职位关联的社招内推官网 id 列表（Moka 原始字段名 recommend_social）
     */
    @JsonProperty("recommend_social")
    private List<Long> recommendSocial;
}
