package com.cacch.integration.convert.meeting;

import com.cacch.integration.common.dto.meeting.MeetingCreateScanResult;
import com.cacch.integration.dto.meeting.request.SaveSmartTableRequest;
import com.cacch.integration.dto.meeting.vo.MeetingCreateScanResultVO;
import com.cacch.integration.dto.meeting.vo.MeetingRecordVO;
import com.cacch.integration.dto.meeting.vo.SmartTableConfigVO;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.entity.meeting.SmartTableDO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 会议模块 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface MeetingConverter {

    SmartTableConfigVO toSmartTableVO(SmartTableDO smartTableDO);

    List<SmartTableConfigVO> toSmartTableVOList(List<SmartTableDO> list);

    SmartTableDO toSmartTableDO(SaveSmartTableRequest request);

    /**
     * 按请求体构造 DO 并携带主键（更新场景，替代手动 {@code setXxx}）
     *
     * @param request 请求体
     * @param id      配置主键
     * @return 携带主键的 DO
     */
    SmartTableDO toSmartTableDO(SaveSmartTableRequest request, Long id);

    MeetingRecordVO toMeetingRecordVO(MeetingRecordDO recordDO);

    List<MeetingRecordVO> toMeetingRecordVOList(List<MeetingRecordDO> list);

    MeetingCreateScanResultVO toMeetingCreateScanResultVO(MeetingCreateScanResult result);
}
