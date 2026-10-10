package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 标记候选人已离职响应体
 *
 * <p>对接 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/resign} 的返回结构：
 * <pre>
 * {
 *   "code": "...",           // string，判断调用结果
 *   "errorMessage": "..."    // string，报错信息
 * }
 * </pre></p>
 *
 * <p>重要：Moka 官方文档明确 {@code success} 字段已废弃，严禁用于业务逻辑判断，
 * 必须使用 {@code code} 判定调用结果，故本 DTO 不定义 success 字段。</p>
 *
 * <p>成功判定说明：Moka 官方文档未列出该接口 {@code code} 的成功枚举值，
 * 参考 Moka 其他接口口径（部门接口 code=0、申请查询接口 code=200），
 * {@link #isSuccess()} 兼容判断 {@code code ∈ {"0", "200", "success"}}；
 * 该接口为归档类写操作，成功时通常无额外业务数据返回，故不定义 data 字段。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaApplicationResignResponse {

    /**
     * 成功码 —— code=0（部门接口口径）
     */
    public static final String CODE_SUCCESS_ZERO = "0";

    /**
     * 成功码 —— code=200（申请查询接口口径）
     */
    public static final String CODE_SUCCESS_200 = "200";

    /**
     * 成功码 —— code=success（文本成功码兜底）
     */
    public static final String CODE_SUCCESS_TEXT = "success";

    /**
     * 返回码（string），用于判断调用结果；Moka 官方文档未列出成功枚举值
     */
    private String code;

    /**
     * 报错信息，调用失败时返回
     */
    private String errorMessage;

    /**
     * 判断调用是否成功（仅以 code 判定，success 字段已被 Moka 废弃）
     *
     * <p>兼容 Moka 各接口成功码口径：code ∈ {"0", "200", "success"} 视为成功。</p>
     *
     * @return code 命中任一成功码时返回 true
     */
    public boolean isSuccess() {
        return CODE_SUCCESS_ZERO.equals(code)
                || CODE_SUCCESS_200.equals(code)
                || CODE_SUCCESS_TEXT.equals(code);
    }
}
