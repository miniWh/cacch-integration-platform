package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 获取人事信息 V3 请求体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MokaUserListV3Request {

    private String limit;
    private String order;
    private Long nextUserId;
    private Integer deactivated;
}