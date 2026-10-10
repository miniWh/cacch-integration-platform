package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * IHR 新增待入职-简略 — 响应体包装
 *
 * <p>通用字段（code / message / errorResult / isSuccess）继承自 {@link IhrBaseResponse}。
 * 业务特有字段：{@code data}（Boolean 添加结果）、{@code entryId}（待入职 id）。</p>
 *
 * <p>接口文档响应结构：
 * <pre>
 * {
 *   "data": true,                  // Boolean：添加结果，成功为 true
 *   "code": 0,                     // int：0 成功（继承自 IhrBaseResponse）
 *   "message": "SUCCESS",          // String（继承自 IhrBaseResponse）
 *   "errorResult": false,          // Boolean（继承自 IhrBaseResponse）
 *   "entryId": "a6fa604a-..."      // String：待入职 id
 * }
 * </pre>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrStaffEntryFormCreateResponse extends IhrBaseResponse {

    /**
     * 添加结果：true 成功 / false 失败
     */
    private Boolean data;

    /**
     * 待入职 id（String UUID，成功时返回）
     */
    private String entryId;
}
