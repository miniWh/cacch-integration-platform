package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 职位查询入参自定义字段过滤项
 *
 * <p>注意：出参侧自定义字段 id 为 integer（见 {@link MokaJobCustomField}），
 * 而本入参过滤项 id 为 string，类型不同故分开定义。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobCustomFieldFilter {

    /**
     * 自定义字段 id
     */
    private String id;

    /**
     * 自定义字段值
     */
    private String value;
}
