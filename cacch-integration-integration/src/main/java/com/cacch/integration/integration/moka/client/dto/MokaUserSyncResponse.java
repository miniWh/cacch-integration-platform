package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka 用户信息同步接口 —— syncInfo 响应体 DTO
 *
 * <p>对接 Moka 开放平台 {@code POST /api-platform/v1/users/syncInfo}
 * （{@linkplain <a href="https://www.mokahr.com/docs/api/#-72">API 文档 #-72</a>}）。
 * Moka 通用响应结构：</p>
 *
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "success",
 *   "data": null
 * }
 * </pre>
 *
 * <p>成功判定兼容 {@code code=0} 与 {@code code=200} 两种口径。
 * syncInfo 接口的 {@code data} 字段在成功时通常为 null，
 * 失败时可能携带错误详情，本 DTO 用 {@link Object} 兜底以兼容未知结构。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaUserSyncResponse {

    /**
     * 业务状态码：0 或 200 表示成功，其他表示失败
     */
    private int code;

    /**
     * 状态描述，成功时通常为 "success"，失败时为错误原因
     */
    private String msg;

    /**
     * 业务数据 —— syncInfo 接口成功时通常为 null，
     * 失败时可能携带错误详情（结构未知）
     */
    private Object data;

    /**
     * 是否成功响应
     */
    public boolean isSuccess() {
        return code == 0 || code == 200;
    }
}
