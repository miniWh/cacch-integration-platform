package com.cacch.integration.dto.ihr.request;

import com.cacch.integration.common.constant.ihr.IhrConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * IHR 部门查询请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class SearchDepartmentRequest {

    /**
     * 页码（从 0 开始），IHR 文档约定首页传 0
     */
    @NotNull
    @Min(0)
    private Integer page;

    /**
     * 每页条数，IHR 接口默认 10
     */
    @NotNull
    @Min(1)
    @Max(200)
    private Integer size;

    /**
     * 搜索条件（可空；空表示全量查询）
     */
    private List<SearchCondition> conditions;

    /**
     * 单条搜索条件
     */
    @Data
    public static class SearchCondition {

        /**
         * 搜索字段名，建议使用 {@link IhrConstants} 中的 SEARCH_KEY_* 常量
         */
        private String searchKey;

        /**
         * 搜索值
         */
        private String searchParam;

        /**
         * 搜索类型，建议使用 {@link IhrConstants} 中的 SEARCH_TYPE_* 常量
         */
        private String searchType;
    }
}
