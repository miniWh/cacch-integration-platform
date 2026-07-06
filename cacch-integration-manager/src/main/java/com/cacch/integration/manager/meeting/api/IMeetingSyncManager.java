package com.cacch.integration.manager.meeting.api;

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
     * 为待发起(PENDING)会议创建企微预约会议并回写智能表格
     */
    void createPendingWeComMeetings();

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
