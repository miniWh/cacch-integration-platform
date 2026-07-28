package com.cacch.integration.convert.oa;

import com.cacch.integration.dto.oa.vo.OaRegAttachmentSyncRecordVO;
import com.cacch.integration.entity.oa.OaRegAttachmentSyncDO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 国内登记报告附件同步 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface OaRegAttachmentSyncConverter {

    /**
     * DO 转 VO
     *
     * @param record 同步记录 DO
     * @return VO
     */
    OaRegAttachmentSyncRecordVO toVO(OaRegAttachmentSyncDO record);

    /**
     * DO 列表转 VO 列表
     *
     * @param records DO 列表
     * @return VO 列表
     */
    List<OaRegAttachmentSyncRecordVO> toVOList(List<OaRegAttachmentSyncDO> records);
}
