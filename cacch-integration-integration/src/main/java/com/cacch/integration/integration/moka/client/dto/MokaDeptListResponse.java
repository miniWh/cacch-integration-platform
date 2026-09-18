package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Moka 组织架构 —— 获取全量组织架构响应体
 *
 * <p>字段匹配 Moka API {@code GET /api-platform/v1/departments} 的响应结构：
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "success",
 *   "data": [ { "id": 1, "name": "总裁办", "departmentCode": "CEO", ... } ]
 * }
 * </pre>
 * 成功判定兼容 {@code code=0} 与 {@code code=200} 两种口径。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptListResponse {

    /**
     * 业务状态码：0 或 200 表示成功，其他表示失败
     */
    private int code;

    /**
     * 状态描述，成功时通常为 "success"，失败时为错误原因
     */
    private String msg;

    /**
     * 业务数据 —— 部门列表；无数据时为空列表或 null
     */
    private List<MokaDeptItem> data;

    /**
     * 是否成功响应
     */
    public boolean isSuccess() {
        return code == 0 || code == 200;
    }
}
