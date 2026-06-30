package com.cacch.integration.convert.meeting;

import com.cacch.integration.dto.meeting.request.SaveSmartTableRequest;
import com.cacch.integration.dto.meeting.vo.MeetingRecordVO;
import com.cacch.integration.dto.meeting.vo.SmartTableConfigVO;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.entity.meeting.SmartTableDO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MeetingConverter {

    SmartTableConfigVO toSmartTableVO(SmartTableDO smartTableDO);

    List<SmartTableConfigVO> toSmartTableVOList(List<SmartTableDO> list);

    SmartTableDO toSmartTableDO(SaveSmartTableRequest request);

    MeetingRecordVO toMeetingRecordVO(MeetingRecordDO recordDO);

    List<MeetingRecordVO> toMeetingRecordVOList(List<MeetingRecordDO> list);
}
