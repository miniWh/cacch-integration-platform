package com.cacch.integration.controller.meeting;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.meeting.MeetingConverter;
import com.cacch.integration.dto.meeting.request.SaveSmartTableRequest;
import com.cacch.integration.dto.meeting.vo.MeetingRecordVO;
import com.cacch.integration.dto.meeting.vo.SmartTableConfigVO;
import com.cacch.integration.manager.meeting.api.IMeetingSyncManager;
import com.cacch.integration.service.meeting.api.IMeetingRecordService;
import com.cacch.integration.service.meeting.api.ISmartTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能表格会议管理 REST 接口 — 配置、会议记录查询与同步手动触发
 *
 * @author hongfu_zhou@cacch.com
 */
@RestController
@RequestMapping("/api/v1/meeting")
@RequiredArgsConstructor
public class MeetingController {

    private final ISmartTableService smartTableService;
    private final IMeetingRecordService meetingRecordService;
    private final IMeetingSyncManager meetingSyncManager;
    private final MeetingConverter meetingConverter;

    /**
     * 查询启用的总控表配置
     *
     * @return 总控表视图对象；未配置时 data 为 null
     */
    @GetMapping("/smart-tables/master")
    public Result<SmartTableConfigVO> getMasterTable() {
        return Result.success(meetingConverter.toSmartTableVO(smartTableService.getEnabledMaster()));
    }

    /**
     * 查询所有启用的会议子表配置
     *
     * @return 会议子表配置列表
     */
    @GetMapping("/smart-tables")
    public Result<List<SmartTableConfigVO>> listMeetingTables() {
        return Result.success(meetingConverter.toSmartTableVOList(
                smartTableService.listEnabledMeetingTables()));
    }

    /**
     * 按 ID 查询智能表格配置
     *
     * @param id 智能表格配置主键
     * @return 配置视图对象
     */
    @GetMapping("/smart-tables/{id}")
    public Result<SmartTableConfigVO> getSmartTable(@PathVariable Long id) {
        return Result.success(meetingConverter.toSmartTableVO(smartTableService.getById(id)));
    }

    /**
     * 新增智能表格配置
     *
     * @param request 配置请求体
     * @return 保存后的配置视图对象
     */
    @PostMapping("/smart-tables")
    public Result<SmartTableConfigVO> createSmartTable(@Valid @RequestBody SaveSmartTableRequest request) {
        return Result.success(meetingConverter.toSmartTableVO(
                smartTableService.saveNew(meetingConverter.toSmartTableDO(request))));
    }

    /**
     * 更新智能表格配置
     *
     * @param id      配置主键
     * @param request 更新请求体
     * @return 更新后的配置视图对象
     */
    @PutMapping("/smart-tables/{id}")
    public Result<SmartTableConfigVO> updateSmartTable(@PathVariable Long id,
                                                       @Valid @RequestBody SaveSmartTableRequest request) {
        var smartTable = meetingConverter.toSmartTableDO(request);
        smartTable.setId(id);
        smartTableService.updateById(smartTable);
        return Result.success(meetingConverter.toSmartTableVO(smartTableService.getById(id)));
    }

    /**
     * 按状态查询会议记录
     *
     * @param status 会议状态码，为空时返回空列表
     * @return 会议记录视图列表
     */
    @GetMapping("/records")
    public Result<List<MeetingRecordVO>> listRecordsByStatus(String status) {
        return Result.success(meetingConverter.toMeetingRecordVOList(
                meetingRecordService.listByStatusOrEmpty(status)));
    }

    /**
     * 查询指定智能表格下的会议记录
     *
     * @param id 智能表格配置主键
     * @return 会议记录视图列表
     */
    @GetMapping("/smart-tables/{id}/records")
    public Result<List<MeetingRecordVO>> listRecordsByTable(@PathVariable Long id) {
        return Result.success(meetingConverter.toMeetingRecordVOList(
                meetingRecordService.listBySmartTableId(id)));
    }

    /**
     * 手动触发总控表扫描（创建员工会议管理表）
     *
     * @return 无数据成功响应
     */
    @PostMapping("/sync/master-scan")
    public Result<Void> triggerMasterScan() {
        meetingSyncManager.scanMasterAndProvision();
        return Result.success(null);
    }

    /**
     * 手动触发会议行同步
     *
     * @return 无数据成功响应
     */
    @PostMapping("/sync/meeting-records")
    public Result<Void> triggerMeetingSync() {
        meetingSyncManager.syncMeetingRecordsFromSheets();
        return Result.success(null);
    }

    /**
     * 手动触发为待处理会议创建企微会议
     *
     * @return 无数据成功响应
     */
    @PostMapping("/sync/create-meetings")
    public Result<Void> triggerCreateMeetings() {
        meetingSyncManager.createPendingWeComMeetings();
        return Result.success(null);
    }

    /**
     * 手动触发待办回写智能表格
     *
     * @return 无数据成功响应
     */
    @PostMapping("/sync/todos")
    public Result<Void> triggerTodoSync() {
        meetingSyncManager.syncTodosToSheet();
        return Result.success(null);
    }
}
