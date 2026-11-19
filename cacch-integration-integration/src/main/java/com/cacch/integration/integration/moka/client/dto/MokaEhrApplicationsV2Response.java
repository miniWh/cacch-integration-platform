package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Moka —— 获取候选人信息 v2（EHR 申请）响应
 *
 * <p>外层响应为 Moka 通用结构 {@code { code, msg, data }}，通用字段继承自 {@link MokaBaseResponse}；
 * 本接口在响应顶层额外返回 {@code next} 分页游标（与 {@code data} 同级，非嵌套在 data 内）。
 * 成功码为 **200**，成功判定见基类 {@link MokaBaseResponse#isSuccess()}。</p>
 *
 * <p>{@code data} 每项含 {@code basicInfo}、{@code jobInfo}、{@code offerInfo} 等嵌套对象；
 * 为兼容字段演进，元素类型使用 {@link JsonNode}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MokaEhrApplicationsV2Response extends MokaBaseResponse<List<JsonNode>> {

    /**
     * 分页游标；有值时下次请求带上以实现翻页
     */
    private String next;
}