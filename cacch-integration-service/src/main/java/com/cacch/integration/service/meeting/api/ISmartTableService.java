package com.cacch.integration.service.meeting.api;

import com.cacch.integration.entity.meeting.SmartTableDO;

import java.util.List;

/**
 * 智能表格配置服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ISmartTableService {

    /**
     * 按主键查询智能表格配置
     *
     * @param id 主键 ID
     * @return 配置实体，不存在时返回 null
     */
    SmartTableDO getById(Long id);

    /**
     * 查询启用的总控表（MASTER 类型且 status=1）
     *
     * @return 总控表配置，未配置时返回 null
     */
    SmartTableDO getEnabledMaster();

    /**
     * 按用户 ID 与文档 ID 查询配置
     *
     * @param userId 企微用户 ID
     * @param docId  文档 docid
     * @return 匹配的配置，不存在时返回 null
     */
    SmartTableDO getByUserIdAndDocId(String userId, String docId);

    /**
     * 查询所有启用的会议子表（MEETING 类型且 status=1）
     *
     * @return 会议子表列表，无数据时返回空列表
     */
    List<SmartTableDO> listEnabledMeetingTables();

    /**
     * 新增智能表格配置（status 为空时默认启用）
     *
     * @param smartTable 待保存实体
     * @return 保存后的实体（含生成的主键）
     */
    SmartTableDO saveNew(SmartTableDO smartTable);

    /**
     * 按主键更新智能表格配置
     *
     * @param smartTable 待更新实体（须含 id）
     */
    void updateById(SmartTableDO smartTable);

    /**
     * 标记同步成功并更新最后同步时间
     *
     * @param smartTableId 智能表格配置主键
     */
    void markSyncSuccess(Long smartTableId);

    /**
     * 标记同步失败并记录错误信息
     *
     * @param smartTableId 智能表格配置主键
     * @param errorMessage 错误摘要
     */
    void markSyncError(Long smartTableId, String errorMessage);
}
