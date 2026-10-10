package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka 开放平台通用响应基类
 *
 * <p>抽取 Moka 各接口响应中重复的通用字段（code / msg）与成功判定逻辑，
 * 业务数据 data 由子类通过泛型参数声明。Moka 通用响应结构：</p>
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "success",
 *   "data": ...
 * }
 * </pre>
 *
 * <p>成功判定兼容 {@code code=0} 与 {@code code=200} 两种口径。
 * 特例：{@link MokaApplicationResignResponse} 的 code 为 String 类型且字段结构不同，
 * 不纳入本基类。</p>
 *
 * @param <T> 业务数据 data 节点类型
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaBaseResponse<T> {

    /**
     * 业务状态码：0 或 200 表示成功，其他表示失败
     */
    private int code;

    /**
     * 状态描述，成功时通常为 "success"，失败时为错误原因
     */
    private String msg;

    /**
     * 业务数据，结构由各接口响应自行定义；无数据时为 null
     */
    private T data;

    /**
     * 是否成功响应（兼容 code=0 与 code=200 两种口径）
     *
     * @return code 命中任一成功码时返回 true
     */
    public boolean isSuccess() {
        return code == 0 || code == 200;
    }
}
