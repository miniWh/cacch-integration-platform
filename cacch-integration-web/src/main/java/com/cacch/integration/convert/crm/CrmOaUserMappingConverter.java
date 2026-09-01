package com.cacch.integration.convert.crm;

import com.cacch.integration.dto.crm.vo.CrmOaUserMappingVO;
import com.cacch.integration.entity.crm.CrmOaUserMappingDO;
import org.mapstruct.Mapper;

/**
 * CRM 模块 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface CrmOaUserMappingConverter {

    CrmOaUserMappingVO toVO(CrmOaUserMappingDO crmOaUserMappingDO);
}
