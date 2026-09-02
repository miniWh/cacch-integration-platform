package com.cacch.integration.dto.ihr.vo;

import lombok.Data;

import java.util.List;

/**
 * IHR 部门分页视图
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class DepartmentPageVO {

    /**
     * 当前页部门列表
     */
    private List<DepartmentVO> departments;

    /**
     * 总记录数
     */
    private Integer totalElements;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 是否已到末页
     */
    private Boolean end;
}
