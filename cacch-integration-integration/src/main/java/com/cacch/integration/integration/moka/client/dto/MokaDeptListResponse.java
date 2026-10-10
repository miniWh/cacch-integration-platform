package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Moka 组织架构 —— 获取全量组织架构响应体
 *
 * <p>字段匹配 Moka API {@code GET /api-platform/v1/departments} 的响应结构，
 * 通用字段（code / msg / isSuccess）继承自 {@link MokaBaseResponse}；
 * 成功判定兼容 {@code code=0} 与 {@code code=200} 两种口径。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MokaDeptListResponse extends MokaBaseResponse<List<MokaDeptItem>> {
}
