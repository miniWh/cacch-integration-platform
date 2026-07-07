package com.cacch.integration.manager.meeting.api;

import com.cacch.integration.common.dto.meeting.MeetingCreateScanResult;

/**
 * 会议智能表格同步编排接口
 *
 * <p>对应 V2 业务流程：总控扫描 → 会议行同步 → 建会回写 → 纪要拉取 → 待办回写</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMeetingSyncManager {

    /**
     * 扫描总控表，为已批准且未建表的员工创建会议管理智能表格
     */
    void scanMasterAndProvision();

    /**
     * 从员工会议智能表格同步会议行到本地库
     */
    void syncMeetingRecordsFromSheets();

    /**
     * 为待发起(PENDING)会议创建企微预约会议并回写智能表格（扫描后按规则校验）
     */
    void createPendingWeComMeetings();

    /**
     * 扫描所有员工会议管理子表，同步待创建会议记录并按规则发起建会
     *
     * @return 扫描与建会统计
     */
    MeetingCreateScanResult scanAndCreatePendingMeetings();

    /**
     * 扫描指定员工会议管理子表，同步待创建会议记录并按规则发起建会
     *
     * @param smartTableId 员工会议表配置主键（table_type=MEETING）
     * @return 扫描与建会统计
     */
    MeetingCreateScanResult scanAndCreatePendingMeetings(Long smartTableId);

    /**
     * 扫描已创建的企微会议详情，若有变更则回写会议管理子表（已开始会议不扫描）
     */
    void syncScheduledMeetingsFromWeCom();

    /**
     * 将未写入智能表格的待办事项添加到待办子表
     */
    void syncTodosToSheet();

    /**
     * 手动初始化指定员工会议管理表的智能表格列，并更新本地列映射
     *
     * @param smartTableId 员工会议表配置主键（table_type=MEETING）
     */
    void initializeMeetingSheetColumns(Long smartTableId);

    /**
     * 手动初始化所有已启用员工会议管理表的智能表格列
     */
    void initializeAllMeetingSheetColumns();
}
