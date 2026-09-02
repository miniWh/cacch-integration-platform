package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * IHR 获取部门清单 v3 — 响应体
 *
 * <p>响应示例：
 * <pre>
 * {
 *   "code": 0,
 *   "message": "OK",
 *   "data": [ { "uuid": "...", "id": "402", "name": "0011", ... } ],
 *   "totalPages": 8131,
 *   "totalElements": 81312,
 *   "end": false,
 *   "errorResultList": false
 * }
 * </pre>
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
     * 部门列表
     */
    private List<IhrDepartment> data;

    /**
     * 总页数
     */
    @JsonProperty("totalPages")
    private Integer totalPages;

    /**
     * 总记录数
     */
    @JsonProperty("totalElements")
    private Integer totalElements;

    /**
     * 是否已到末页
     */
    private Boolean end;

    /**
     * 是否有错误明细
     */
    @JsonProperty("errorResultList")
    private Boolean errorResultList;

    /**
     * 是否成功响应
     */
    public boolean isSuccess() {
        return code == 0;
    }
}
