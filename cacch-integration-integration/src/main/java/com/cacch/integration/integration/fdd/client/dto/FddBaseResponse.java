package com.cacch.integration.integration.fdd.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 法大大开放平台通用响应基类
 *
 * <p>抽取法大大各接口响应中重复的通用字段（code / message / timestamp）。
 * 业务数据 data 字段结构各接口差异较大（部分接口 data 兼容 object/array，
 * 需专属 {@code @JsonDeserialize} 注解），故 data 由各子类自行声明。</p>
 *
 * <p>成功判定说明：法大大业务接口成功码为 {@code FddConstants.SUCCESS_CODE}（100000），
 * Token 接口成功码为 {@code FddConstants.TOKEN_SUCCESS_CODE}（0），且各接口均要求
 * 校验 data 内容（如 URL / accountId 非空），故 isSuccess() 等判定方法保留在各子类中，
 * 不在基类统一提供。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddBaseResponse {

    /**
     * 业务状态码：业务接口成功为 100000（SUCCESS_CODE），Token 接口成功为 0（TOKEN_SUCCESS_CODE）
     */
    private Integer code;

    /**
     * 状态描述，成功时通常为 "success"，失败时为错误原因
     */
    private String message;

    /**
     * 响应时间戳（毫秒）
     */
    private Long timestamp;
}
