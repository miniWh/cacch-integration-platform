package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;

/**
 * IHR 新增待入职-简略 — 响应体包装
 *
 * <p>接口文档响应结构：
 * <pre>
 * {
 *   "data": true,                  // Boolean：添加结果，成功为 true
 *   "code": 0,                     // int：0 成功
 *   "message": "SUCCESS",          // String
 *   "errorResult": false,          // Boolean
 *   "entryId": "a6fa604a-..."      // String：待入职 id
 * }
 * </pre>
 *
 * <p>成功判定：{@code code == 0}（与部门清单 v3、职位清单一致）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrStaffEntryFormCreateResponse {

    /**
     * 业务状态码：0 表示成功，其它表示失败
     */
    private int code;

    /**
     * 状态描述（成功时为 "SUCCESS"）
     */
    private String message;

    /**
     * 是否有错误明细
     */
    private Boolean errorResult;

    /**
     * 添加结果：true 成功 / false 失败
     */
    private Boolean data;

    /**
     * 待入职 id（String UUID，成功时返回）
     */
    private String entryId;

    /**
     * 是否成功响应（code == 0）
     */
    public boolean isSuccess() {
        return code == 0;
    }
}
