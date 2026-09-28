package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * IHR 获取公司职位清单 — 响应体包装
 *
 * <p>接口文档响应结构：
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "errorResult": false,
 *   "data": {
 *     "resultArray": [ { ...IhrPosition... } ]
 *   }
 * }
 * </pre>
 *
 * <p>注意：与部门清单 v3 不同，本接口无分页，结果直接返回在 {@code data.resultArray} 中。
 * 成功判定：{@code code == 200}（部门接口为 {@code code == 0}）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrPositionListResponse {

    /**
     * 业务状态码：200 表示成功，其它表示失败
     */
    private int code;

    /**
     * 状态描述
     */
    private String message;

    /**
     * 是否有错误明细（实测字段名为 errorResult）
     */
    @JsonProperty("errorResult")
    private Boolean errorResult;

    /**
     * 响应数据对象（含 resultArray）
     */
    private PositionData data;

    /**
     * 是否成功响应（code == 200）
     */
    public boolean isSuccess() {
        return code == 200;
    }

    /**
     * 职位列表（平铺便捷方法，取自 {@code data.resultArray}）
     */
    public List<IhrPosition> getData() {
        return data == null ? null : data.getResultArray();
    }

    /**
     * 响应 data 节点结构
     */
    @Data
    public static class PositionData {

        /**
         * 职位记录数组
         */
        private List<IhrPosition> resultArray;
    }
}
