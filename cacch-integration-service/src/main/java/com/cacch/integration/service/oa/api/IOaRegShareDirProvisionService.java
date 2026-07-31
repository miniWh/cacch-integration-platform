package com.cacch.integration.service.oa.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.entity.oa.OaRegShareDirProvisionDO;

import java.util.List;

/**
 * 共享盘目录治理记录服务（REQ-OA-002）
 *
 * <p>治理记录采用 append 模式，每轮执行新增记录、保留全部历史。
 * 本服务提供批量写入、按轮次查询、统计聚合及管理端分页查询能力。
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaRegShareDirProvisionService {

    /**
     * 批量写入治理记录（单轮 run_id 下所有记录一次性插入）
     *
     * @param records 治理记录列表；为空或 null 时直接返回
     * @return 实际插入条数
     */
    int batchInsert(List<OaRegShareDirProvisionDO> records);

    /**
     * 按 run_id 查询全部治理记录（用于本轮统计与企微通知）
     *
     * @param runId 执行轮次标识
     * @return 治理记录列表；runId 为空时返回空列表
     */
    List<OaRegShareDirProvisionDO> findByRunId(String runId);

    /**
     * 按 run_id 统计指定 action 的记录数
     *
     * @param runId  执行轮次标识
     * @param action 治理动作（如 CREATED、DELETED、SKIPPED_EXISTS、FAILED）
     * @return 记录数；runId 或 action 为空时返回 0
     */
    long countByRunIdAndAction(String runId, String action);

    /**
     * 管理端分页查询治理记录（条件均可选，多条件 AND）
     *
     * @param runId     执行轮次标识，可空
     * @param ownerName 登记负责人，可空；非空时模糊匹配
     * @param action    治理动作，可空；非空时精确匹配
     * @param page      页码，从 1 开始
     * @param size      每页条数
     * @return 分页结果，按 provisionedAt、id 倒序
     */
    IPage<OaRegShareDirProvisionDO> pageQuery(String runId,
                                              String ownerName,
                                              String action,
                                              long page,
                                              long size);
}
