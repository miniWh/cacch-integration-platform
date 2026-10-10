package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Moka ATS —— 职位查询响应
 *
 * <p>外层响应为 Moka 通用结构 { code, msg, data }，通用字段继承自 {@link MokaBaseResponse}，
 * 成功判定兼容 code=0 与 code=200 两种口径。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MokaJobListResponse extends MokaBaseResponse<MokaJobData> {
}
