package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 勤策查询自定义对象数据请求参数
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmCustomObjectQueryRequest {

    /**
     * 对象 key，必填
     */
    @JsonProperty("object_key")
    @JsonAlias("objectKey")
    private String objectKey;

    /**
     * 页码，从 1 开始；为空时服务端默认 1
     */
    private Integer page;

    /**
     * 每页条数；为空时服务端默认 100
     */
    private Integer rows;

    /**
     * 排序集合；为空时默认按创建时间倒序
     */
    private List<CrmSortItem> sorts;

    /**
     * 查询条件组集合
     */
    @JsonProperty("query_group")
    @JsonAlias("queryGroup")
    private List<CrmQueryGroup> queryGroup;
}
