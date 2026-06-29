package com.cacch.integration.convert.wecom;

import com.cacch.integration.dto.wecom.vo.SmartFieldListVO;
import com.cacch.integration.dto.wecom.vo.SmartFieldVO;
import com.cacch.integration.dto.wecom.vo.SmartRecordListVO;
import com.cacch.integration.dto.wecom.vo.SmartRecordVO;
import com.cacch.integration.dto.wecom.vo.SmartSheetVO;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComFieldInfo;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordInfo;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComSheetInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 企微智能表格 MapStruct 转换器
 *
 * @author cacch-integration
 */
@Mapper(componentModel = "spring")
public interface SmartSheetConverter {

    List<SmartSheetVO> toSheetVOList(List<WeComSheetInfo> sheets);

    SmartSheetVO toSheetVO(WeComSheetInfo sheet);

    default List<SmartSheetVO> toSheetVOList(WeComGetSheetResponse response) {
        if (response == null || response.getSheetList() == null) {
            return List.of();
        }
        return toSheetVOList(response.getSheetList());
    }

    SmartFieldVO toFieldVO(WeComFieldInfo field);

    List<SmartFieldVO> toFieldVOList(List<WeComFieldInfo> fields);

    @Mapping(source = "hasMore", target = "hasMore")
    SmartFieldListVO toFieldListVO(WeComGetFieldsResponse response);

    SmartRecordVO toRecordVO(WeComRecordInfo record);

    List<SmartRecordVO> toRecordVOList(List<WeComRecordInfo> records);

    @Mapping(source = "hasMore", target = "hasMore")
    SmartRecordListVO toRecordListVO(WeComGetRecordsResponse response);
}
