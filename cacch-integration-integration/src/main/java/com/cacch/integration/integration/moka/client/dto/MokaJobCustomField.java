package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 职位查询响应中的职位自定义字段
 *
 * <p>注意：本类为出参结构，id 为 integer；入参过滤项 id 为 string，
 * 见 {@link MokaJobCustomFieldFilter}，两者类型不同故分开定义。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobCustomField {

    /**
     * 自定义字段 id
     */
    private Integer id;

    /**
     * 自定义字段名称
     */
    private String name;

    /**
     * 自定义字段类型：string_info / bool_info
     */
    private String type;

    /**
     * 自定义字段的值
     */
    private String value;
}
