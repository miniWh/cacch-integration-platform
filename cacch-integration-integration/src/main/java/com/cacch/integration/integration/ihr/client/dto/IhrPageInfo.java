package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;

/**
 * IHR 接口通用分页信息
 *
 * <p>iHR 多个分页接口的响应体均包含 pageInfo 结构，抽取后复用于员工 ID 清单等接口。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrPageInfo {

    /**
     * 总记录数
     */
    private Long totalCount;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 当前页码
     */
    private Integer pageNo;

    /**
     * 每页条数
     */
    private Integer pageSize;
}
