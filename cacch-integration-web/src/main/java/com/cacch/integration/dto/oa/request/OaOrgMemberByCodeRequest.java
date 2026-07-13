package com.cacch.integration.dto.oa.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按编码取 OA 人员请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaOrgMemberByCodeRequest {

    /**
     * 人员编号（对应 CRM emp_code），必填
     */
    @NotBlank(message = "code 不能为空")
    private String code;

    /**
     * 页号，默认 0
     */
    private Integer pageNo;

    /**
     * 每页条数，默认 20
     */
    private Integer pageSize;

    /**
     * Token 绑定登录名，可空
     */
    private String loginName;
}
