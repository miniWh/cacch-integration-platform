package com.cacch.integration.common.dto.oa;

import lombok.Builder;
import lombok.Data;

/**
 * 共享盘目录治理执行结果统计（REQ-OA-002）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class OaRegShareDirProvisionResult {

    /**
     * 本轮处理的资料行数
     */
    private int totalRows;

    /**
     * L3 路径组数（去重后）
     */
    private int totalGroups;

    /**
     * 创建目录数
     */
    private int created;

    /**
     * 删除空目录数
     */
    private int deleted;

    /**
     * 已存在跳过数
     */
    private int skippedExists;

    /**
     * 非空目录跳过数
     */
    private int skippedNotEmpty;

    /**
     * 不需要且目录不存在跳过数
     */
    private int skippedNotRequired;

    /**
     * 组内保留跳过数（不需要但同组其他行需要）
     */
    private int skippedGroupRetained;

    /**
     * 失败数（归一化失败/SMB异常等）
     */
    private int failed;
}
