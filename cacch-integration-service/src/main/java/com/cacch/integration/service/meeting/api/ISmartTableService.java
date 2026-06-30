package com.cacch.integration.service.meeting.api;

import com.cacch.integration.entity.meeting.SmartTableDO;

import java.util.List;

/**
 * 智能表格配置服务
 */
public interface ISmartTableService {

    SmartTableDO getById(Long id);

    SmartTableDO getEnabledMaster();

    SmartTableDO getByUserIdAndDocId(String userId, String docId);

    List<SmartTableDO> listEnabledMeetingTables();

    void save(SmartTableDO smartTable);

    void updateById(SmartTableDO smartTable);
}
