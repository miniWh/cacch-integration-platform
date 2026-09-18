package com.cacch.integration.service.moka.api;

import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncResponse;

/**
 * Moka 组织架构服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaOrgService {

    /**
     * 组织架构全量同步 —— 调用 Moka {@code PUT /api-platform/v2/departments}
     *
     * <p>同步以 departmentCode 为主键，Moka 侧自动执行新增 / 更新 / 标记删除。</p>
     *
     * @param request 同步请求体，不可为空；departments 列表必填
     * @return Moka 同步结果响应（含 new / update / delete 数量）
     * @throws com.cacch.integration.common.exception.BizException Moka API Key 未配置、
     *                                                             HTTP 调用失败或 Moka 返回业务失败时抛出
     */
    MokaDeptSyncResponse syncDepartmentsFull(MokaDeptSyncRequest request);
}
