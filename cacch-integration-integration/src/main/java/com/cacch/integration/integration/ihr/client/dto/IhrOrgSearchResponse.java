package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
 * <p>通用字段（code / message / errorResult / isSuccess）继承自 {@link IhrBaseResponse}。
 * 注意：{@code data} 是分页对象而非裸数组，部门记录位于 {@code data.content}；
 * 本类通过顶层便捷方法（{@link #getData()}、{@link #getTotalElements()}、{@link #getEnd()}）
 * 将分页属性平铺暴露，调用方无需感知嵌套结构。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrOrgSearchResponse extends IhrBaseResponse {

    /**
     * 分页数据对象（含部门列表 content 与分页元信息）
     */
    private PageData data;

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
