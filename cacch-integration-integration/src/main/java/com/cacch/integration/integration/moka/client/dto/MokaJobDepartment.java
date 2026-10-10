package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 职位查询响应中的职位所在部门信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobDepartment {

    /**
     * 部门 id
     */
    private Integer id;

    /**
     * 部门编码
     */
    private Integer code;

    /**
     * 部门名称
     */
    private String name;
}
