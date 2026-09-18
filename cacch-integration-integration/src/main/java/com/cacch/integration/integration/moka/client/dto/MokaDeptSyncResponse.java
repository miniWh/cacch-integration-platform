package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Moka 组织架构全量同步 —— 响应体
 *
 * <p>字段严格匹配 Moka API {@code PUT /api-platform/v2/departments} 的响应结构：
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "success",
 *   "data": {
 *     "result": { "new": 0, "delete": 0, "update": 0 }
 *   }
 * }
 * </pre>
 * 成功判定同时兼容文档示例的 {@code code=0} 与返回字段表的 {@code code=200} 两种口径。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptSyncResponse {

    /**
     * 业务状态码：0 或 200 表示成功，其他表示失败
     */
    private int code;

    /**
     * 状态描述，成功时通常为 "success"，失败时为错误原因
     */
    private String msg;

    /**
     * 业务数据 —— 嵌套 result 对象
     */
    private SyncResultData data;

    /**
     * 是否成功响应
     */
    public boolean isSuccess() {
        return code == 0 || code == 200;
    }

    /**
     * 新增部门数量（data.result.new）；data 为 null 时返回 null
     */
    public Integer getNewCount() {
        return data == null || data.getResult() == null ? null : data.getResult().getNewCount();
    }

    /**
     * 更新部门数量（data.result.update）；data 为 null 时返回 null
     */
    public Integer getUpdateCount() {
        return data == null || data.getResult() == null ? null : data.getResult().getUpdate();
    }

    /**
     * 标记删除部门数量（data.result.delete）；data 为 null 时返回 null
     */
    public Integer getDeleteCount() {
        return data == null || data.getResult() == null ? null : data.getResult().getDelete();
    }

    /**
     * Moka 响应 data 节点 —— 包含 result 统计对象
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class SyncResultData {

        /**
         * 同步结果统计对象
         */
        private SyncResult result;
    }

    /**
     * 同步结果统计 —— 新增 / 更新 / 删除的部门数量
     *
     * <p>JSON 字段名与 Moka API 原始响应一致：{@code new} / {@code update} / {@code delete}；
     * 其中 {@code new} 是 Java 保留字，用 {@code @JsonProperty} 映射。</p>
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class SyncResult {

        /**
         * 本次同步 Moka 侧新增的部门数量（JSON 字段名 "new"，Java 保留字需 @JsonProperty）
         */
        @JsonProperty("new")
        private Integer newCount;

        /**
         * 本次同步 Moka 侧更新的部门数量
         */
        private Integer update;

        /**
         * 本次同步 Moka 侧标记删除的部门数量
         */
        private Integer delete;
    }
}
