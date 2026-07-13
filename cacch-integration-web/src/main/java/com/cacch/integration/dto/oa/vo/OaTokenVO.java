package com.cacch.integration.dto.oa.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OA Token 联调返回
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OaTokenVO {

    /**
     * Token 值
     */
    private String token;
}
