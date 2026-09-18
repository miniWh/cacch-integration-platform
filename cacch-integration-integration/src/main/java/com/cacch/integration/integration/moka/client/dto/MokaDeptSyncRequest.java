package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Moka 组织架构全量同步 —— 请求体
 *
 * <p>字段严格匹配 Moka API {@code PUT /api-platform/v2/departments} 的请求结构。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptSyncRequest {

    /**
     * 部门数据列表（必填）
     */
    private List<MokaDepartment> departments;

    /**
     * 系统内操作人邮箱 —— 仅用于 Moka 侧日志记录（可选）
     */
    private String operatorEmail;
}
