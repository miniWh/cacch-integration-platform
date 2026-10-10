package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Moka 用户信息同步接口 —— syncInfo 响应体 DTO
 *
 * <p>对接 Moka 开放平台 {@code POST /api-platform/v1/users/syncInfo}
 * （{@linkplain <a href="https://www.mokahr.com/docs/api/#-72">API 文档 #-72</a>}），
 * 通用字段（code / msg / isSuccess）继承自 {@link MokaBaseResponse}。</p>
 *
 * <p>syncInfo 接口的 {@code data} 字段在成功时通常为 null，
 * 失败时可能携带错误详情，本 DTO 用 {@link Object} 兜底以兼容未知结构。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MokaUserSyncResponse extends MokaBaseResponse<Object> {
}
