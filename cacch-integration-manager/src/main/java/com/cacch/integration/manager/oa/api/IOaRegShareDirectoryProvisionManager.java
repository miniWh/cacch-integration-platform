package com.cacch.integration.manager.oa.api;

import com.cacch.integration.common.dto.oa.OaRegShareDirProvisionResult;

/**
 * 共享盘目录治理编排（REQ-OA-002）
 *
 * <p>OA 期望态驱动：读取 OA 资料列表（含「需要 / 不需要」标记），
 * 按 L3 路径分组聚合后探测共享盘，按需创建缺失目录、安全删除空 L3。
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaRegShareDirectoryProvisionManager {

    /**
     * 执行一轮目录治理
     *
     * @param formMainId 可选主表 ID 过滤；null 表示按游标全量分批
     * @return 本轮统计
     */
    OaRegShareDirProvisionResult provisionDirectories(Long formMainId);
}
