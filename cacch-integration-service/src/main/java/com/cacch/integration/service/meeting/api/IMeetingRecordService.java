package com.cacch.integration.service.meeting.api;

import com.cacch.integration.entity.meeting.MeetingRecordDO;

import java.util.List;

/**
 * 会议记录服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMeetingRecordService {

    /**
     * 按主键查询会议记录
     *
     * @param id 会议记录主键
     * @return 会议记录，不存在时返回 null
     */
    MeetingRecordDO getById(Long id);

    /**
     * 按智能表格 ID 与企微行 recordId 查询
     *
     * @param smartTableId 智能表格配置主键
     * @param recordId     企微智能表格行 ID
     * @return 会议记录，不存在时返回 null
     */
    MeetingRecordDO getBySmartTableIdAndRecordId(Long smartTableId, String recordId);

    /**
     * 查询指定智能表格下的全部会议记录
     *
     * @param smartTableId 智能表格配置主键
     * @return 会议记录列表
     */
    List<MeetingRecordDO> listBySmartTableId(Long smartTableId);

    /**
     * 按状态查询会议记录
     *
     * @param status 会议状态码，见 {@link com.cacch.integration.common.enums.meeting.MeetingRecordStatusEnum}
     * @return 会议记录列表
     */
    List<MeetingRecordDO> listByStatus(String status);

    /**
     * 按状态查询；status 为空时返回空列表
     *
     * @param status 会议状态码，可为 null
     * @return 会议记录列表，status 为空时返回空列表
     */
    List<MeetingRecordDO> listByStatusOrEmpty(String status);

    /**
     * 新增会议记录
     *
     * @param record 待保存实体
     */
    void save(MeetingRecordDO record);

    /**
     * 按主键更新会议记录
     *
     * @param record 待更新实体（须含 id）
     */
    void updateById(MeetingRecordDO record);

    /**
     * 查询已创建且含企微会议 ID 的记录（用于企微详情反向同步）
     *
     * @param status 会议状态码
     * @return 会议记录列表
     */
    List<MeetingRecordDO> listByStatusWithWecomMeetingId(String status);
}
