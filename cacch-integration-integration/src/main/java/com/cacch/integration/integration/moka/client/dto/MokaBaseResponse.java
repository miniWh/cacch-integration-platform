package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * Moka 开放平台通用响应基类
 *
 * <p>抽取 Moka 各接口响应中重复的通用字段（code / 描述字段 / success）与成功判定逻辑，
 * 业务数据 data 由子类通过泛型参数声明。Moka 各接口响应结构统一：</p>
 * <pre>
 * {
 *   "code": 0,
 *   "message"/"msg": "操作成功",
 *   "success": true,
 *   "data": ...
 * }
 * </pre>
 *
 * <p><strong>注意</strong>：Moka 不同接口描述字段名不一致（职位查询用 {@code message}，
 * Offer 扩展用 {@code msg}），通过 {@link JsonAlias} 同时兼容两个字段名，
 * 统一映射到 Java 字段 {@code msg}。</p>
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
     * 状态描述，成功时通常为 "success"/"操作成功"，失败时为错误原因
     *
     * <p>Moka API 不同接口描述字段名不一致：部分接口返回 {@code message}，
     * 部分返回 {@code msg}。通过 {@link JsonAlias} 兼容两者，均映射到此字段。</p>
     */
    @JsonAlias("message")
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
