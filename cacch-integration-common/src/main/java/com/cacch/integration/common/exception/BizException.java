package com.cacch.integration.common.exception;

import com.cacch.integration.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常基类，所有自定义异常均继承此类
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 错误码枚举
     */
    private final ResultCode resultCode;

    /**
     * 通过错误码创建异常
     */
    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /**
     * 通过错误码 + 自定义消息创建异常
     */
    public BizException(ResultCode resultCode, String detailMessage) {
        super(detailMessage);
        this.resultCode = resultCode;
    }

    /**
     * 通过错误码 + 自定义消息 + 原始异常创建异常
     */
    public BizException(ResultCode resultCode, String detailMessage, Throwable cause) {
        super(detailMessage, cause);
        this.resultCode = resultCode;
    }

    public int getCode() {
        return resultCode.getCode();
    }
}
