package com.cacch.integration.convert.oa;

import com.cacch.integration.dto.oa.vo.OaContractAttachmentSyncRecordVO;
import com.cacch.integration.entity.oa.OaContractAttachmentSyncDO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * OA 货物购销合同附件回传 CRM MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface OaContractAttachmentSyncConverter {

    /**
     * DO 转 VO
     *
     * @param record 中间记录 DO
     * @return 同步状态 VO
     */
    OaContractAttachmentSyncRecordVO toVO(OaContractAttachmentSyncDO record);

    /**
     * DO 列表转 VO 列表
     *
     * @param records DO 列表
     * @return VO 列表
     */
    List<OaContractAttachmentSyncRecordVO> toVOList(List<OaContractAttachmentSyncDO> records);
}
