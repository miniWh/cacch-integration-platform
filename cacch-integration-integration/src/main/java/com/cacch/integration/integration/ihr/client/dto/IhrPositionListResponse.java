package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * IHR 获取公司职位清单 — 响应体包装
 *
 * <p>注意：
 * <ul>
 *     <li>成功判定：{@code code == 0}（与部门清单 v3 一致）</li>
 *     <li>{@code data} 直接是 JSONArray，不是嵌套对象（不同于部门清单 v3 的 data.content 结构）</li>
 *     <li>无分页，一次返回全部职位</li>
 * </ul>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrPositionListResponse {

    /**
     * 业务状态码：0 表示成功，其它表示失败
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
     * 职位列表（直接是 JSONArray，非分页包装）
     */
    private List<IhrPosition> data;

    /**
     * 是否成功响应
     */
    public boolean isSuccess() {
        return code == 0;
    }
}
