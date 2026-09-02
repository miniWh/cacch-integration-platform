package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * IHR 获取部门清单 v3 — 请求体
 *
 * <p>请求示例：
 * <pre>
 * {
 *   "page": 0,
 *   "size": 10,
 *   "searchArgsList": [
 *     {
 *       "searchKey": "departmentName",
 *       "searchParam": "0001",
 *       "searchType": "EQUAL"
 *     }
 *   ]
 * }
 * </pre>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrOrgSearchRequest {

    /**
     * 页码（从 0 开始），IHR 文档约定首页传 0
     */
    private int page;

    /**
     * 每页条数
     */
    private int size;

    /**
     * 搜索条件列表；为空表示全量查询
     */
    @JsonProperty("searchArgsList")
    private List<SearchArg> searchArgsList;

    /**
     * 搜索条件
     */
    @Data
    public static class SearchArg {

        /**
         * 搜索字段名，取值参见 {@link com.cacch.integration.common.constant.ihr.IhrConstants}
         * 中的 SEARCH_KEY_* 常量
         */
        private String searchKey;

        /**
         * 搜索值
         */
        private String searchParam;

        /**
         * 搜索类型，取值：EQUAL / LIKE / FUZZY / IN
         */
        private String searchType;
    }
}
