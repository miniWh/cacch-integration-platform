package com.cacch.integration.controller.meeting;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.meeting.MeetingConverter;
import com.cacch.integration.dto.meeting.request.SaveSmartTableRequest;
import com.cacch.integration.dto.meeting.vo.MeetingRecordVO;
import com.cacch.integration.dto.meeting.vo.SmartTableConfigVO;
import com.cacch.integration.entity.meeting.SmartTableDO;
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
 * 智能表格会议管理 — 配置与会议记录查询
 */
@RestController
@RequestMapping("/api/v1/meeting")
@RequiredArgsConstructor
public class MeetingController {

    private final ISmartTableService smartTableService;
    private final IMeetingRecordService meetingRecordService;
    private final IMeetingSyncManager meetingSyncManager;
    private final MeetingConverter meetingConverter;

    @GetMapping("/smart-tables/master")
    public Result<SmartTableConfigVO> getMasterTable() {
        SmartTableDO master = smartTableService.getEnabledMaster();
        return Result.success(master != null ? meetingConverter.toSmartTableVO(master) : null);
    }

    @GetMapping("/smart-tables")
    public Result<List<SmartTableConfigVO>> listMeetingTables() {
        return Result.success(meetingConverter.toSmartTableVOList(
                smartTableService.listEnabledMeetingTables()));
    }

    @GetMapping("/smart-tables/{id}")
    public Result<SmartTableConfigVO> getSmartTable(@PathVariable Long id) {
        return Result.success(meetingConverter.toSmartTableVO(smartTableService.getById(id)));
    }

    @PostMapping("/smart-tables")
    public Result<SmartTableConfigVO> createSmartTable(@Valid @RequestBody SaveSmartTableRequest request) {
        SmartTableDO smartTable = meetingConverter.toSmartTableDO(request);
        if (smartTable.getStatus() == null) {
            smartTable.setStatus(1);
        }
        smartTableService.save(smartTable);
        return Result.success(meetingConverter.toSmartTableVO(smartTable));
    }

    @PutMapping("/smart-tables/{id}")
    public Result<SmartTableConfigVO> updateSmartTable(@PathVariable Long id,
                                                           @Valid @RequestBody SaveSmartTableRequest request) {
        SmartTableDO smartTable = meetingConverter.toSmartTableDO(request);
        smartTable.setId(id);
        smartTableService.updateById(smartTable);
        return Result.success(meetingConverter.toSmartTableVO(smartTableService.getById(id)));
    }

    @GetMapping("/records")
    public Result<List<MeetingRecordVO>> listRecordsByStatus(String status) {
        if (status == null) {
            return Result.success(List.of());
        }
        return Result.success(meetingConverter.toMeetingRecordVOList(
                meetingRecordService.listByStatus(status)));
    }

    @GetMapping("/smart-tables/{id}/records")
    public Result<List<MeetingRecordVO>> listRecordsByTable(@PathVariable Long id) {
        return Result.success(meetingConverter.toMeetingRecordVOList(
                meetingRecordService.listBySmartTableId(id)));
    }

    @PostMapping("/sync/master-scan")
    public Result<Void> triggerMasterScan() {
        meetingSyncManager.scanMasterAndProvision();
        return Result.success(null);
    }

    @PostMapping("/sync/meeting-records")
    public Result<Void> triggerMeetingSync() {
        meetingSyncManager.syncMeetingRecordsFromSheets();
        return Result.success(null);
    }

    @PostMapping("/sync/create-meetings")
    public Result<Void> triggerCreateMeetings() {
        meetingSyncManager.createPendingWeComMeetings();
        return Result.success(null);
    }

    @PostMapping("/sync/todos")
    public Result<Void> triggerTodoSync() {
        meetingSyncManager.syncTodosToSheet();
        return Result.success(null);
    }
}
