package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * IHR 获取部门清单 v3 — 响应体
 *
 * <p>实测响应结构（2026-09-16，网关 openapi.cacch.com）：
 * <pre>
 * {
 *   "code": 0,
 *   "message": "OK",
 *   "errorResult": false,
 *   "data": {
 *     "totalPages": 570,
 *     "totalElements": 1140,
 *     "end": false,
 *     "content": [ { "uuid": "...", "id": 3194, "name": "...", ... } ]
 *   }
 * }
 * </pre>
 *
 * <p>注意：{@code data} 是分页对象而非裸数组，部门记录位于 {@code data.content}；
 * 本类通过顶层便捷方法（{@link #getData()}、{@link #getTotalElements()}、{@link #getEnd()}）
 * 将分页属性平铺暴露，调用方无需感知嵌套结构。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrOrgSearchResponse {

    /**
     * 业务状态码：0 表示成功，其它表示失败
     */
    private int code;

    /**
     * 状态描述
     */
    private String message;

    /**
     * 是否有错误明细（实测字段名为 errorResult，非 errorResultList）
     */
    @JsonProperty("errorResult")
    private Boolean errorResult;

    /**
     * 分页数据对象（含部门列表 content 与分页元信息）
     */
    private PageData data;

    /**
     * 是否成功响应
     */
    public boolean isSuccess() {
        return code == 0;
    }

    /**
     * 部门列表（平铺便捷方法，取自 {@code data.content}）
     */
    public List<IhrDepartment> getData() {
        return data == null ? null : data.getContent();
    }

    /**
     * 总记录数（平铺便捷方法）
     */
    public Integer getTotalElements() {
        return data == null ? null : data.getTotalElements();
    }

    /**
     * 总页数（平铺便捷方法）
     */
    public Integer getTotalPages() {
        return data == null ? null : data.getTotalPages();
    }

    /**
     * 是否已到末页（平铺便捷方法）
     */
    public Boolean getEnd() {
        return data == null ? null : data.getEnd();
    }

    /**
     * IHR 分页对象结构（data 节点）
     */
    @Data
    public static class PageData {

        /**
         * 当前页部门记录
         */
        private List<IhrDepartment> content;

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
}
