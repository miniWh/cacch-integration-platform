package com.cacch.integration.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(0, "操作成功"),

    // ———————————————— 客户端错误 4xxxx ————————————————
    PARAM_ERROR(40000, "参数校验失败"),
    PARAM_MISSING(40001, "缺少必要参数"),
    PARAM_INVALID(40002, "参数无效"),

    // ———————————————— 集成层错误 5xxxx ————————————————
    INTEGRATION_ERROR(50001, "第三方系统调用失败"),
    INTEGRATION_TIMEOUT(50002, "第三方系统调用超时"),
    INTEGRATION_AUTH_FAILED(50003, "第三方系统鉴权失败"),

    // ———————————————— 系统错误 9xxxx ————————————————
    SYSTEM_ERROR(99999, "系统内部异常");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误描述
     */
    private final String message;
}
