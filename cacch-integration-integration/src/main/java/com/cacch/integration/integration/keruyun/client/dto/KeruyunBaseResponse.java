package com.cacch.integration.integration.keruyun.client.dto;

import com.cacch.integration.common.constant.keruyun.KeruyunCommonConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 客如云开放平台通用响应基类
 *
 * <p>抽取客如云各接口响应中重复的通用字段（code / message / messageUuid）与成功判定逻辑，
 * 业务数据 result 由子类通过泛型参数声明。响应结构：</p>
 * <pre>
 * {
 *   "code": 0,
 *   "message": "success",
 *   "messageUuid": "...",
 *   "result": ...
 * }
 * </pre>
 *
 * <p>成功判定统一按 {@code code=0}。</p>
 *
 * @param <T> 业务数据 result 节点类型
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeruyunBaseResponse<T> {

    /**
     * 业务返回码，0 表示成功
     */
    private Integer code;

    /**
     * 返回说明
     */
    private String message;

    /**
     * 消息 UUID，便于与客如云排查问题
     */
    private String messageUuid;

    /**
     * 业务数据
     */
    private T result;

    /**
     * 是否成功响应（code == 0）
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return code != null && code == KeruyunCommonConstants.SUCCESS_CODE;
    }
}
