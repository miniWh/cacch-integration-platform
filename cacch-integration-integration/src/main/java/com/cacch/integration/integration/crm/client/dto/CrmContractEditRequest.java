package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 勤策 CRM 修改销售合同（contractEdit）请求体（REQ-OA-003）
 *
 * <p>响应复用 {@link CrmOpenApiResponse} 信封，无独立响应 DTO。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmContractEditRequest {

    /**
     * CRM 合同数据 ID（formmain_2817.field0495）
     */
    @JsonProperty("data_id")
    private String dataId;

    /**
     * 待修改字段集合：key 为字段标识（含 {@code id} 与附件字段 key），
     * value 为标量值或对象数组（附件字段为 [{path, name}] 列表，不传 fileSize）
     */
    private Map<String, Object> fields;

    /**
     * 是否触发业务规则，固定 false
     */
    @JsonProperty("is_trigger")
    private Boolean isTrigger;

    /**
     * 是否校验必填，固定 false
     */
    @JsonProperty("check_rule")
    private Boolean checkRule;
}
