package com.cacch.integration.convert.oa;

import com.cacch.integration.dto.oa.vo.OaRegShareDirProvisionRecordVO;
import com.cacch.integration.entity.oa.OaRegShareDirProvisionDO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 共享盘目录治理记录 MapStruct 转换器（REQ-OA-002）
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface OaRegShareDirProvisionConverter {

    /**
     * DO 转 VO
     *
     * @param record 治理记录 DO
     * @return VO
     */
    OaRegShareDirProvisionRecordVO toVO(OaRegShareDirProvisionDO record);

    /**
     * DO 列表转 VO 列表
     *
     * @param records DO 列表
     * @return VO 列表
     */
    List<OaRegShareDirProvisionRecordVO> toVOList(List<OaRegShareDirProvisionDO> records);
}
