package com.cacch.integration.mapper.crm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.crm.CrmOaUserMappingDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CRM↔OA 人员映射 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface CrmOaUserMappingMapper extends BaseMapper<CrmOaUserMappingDO> {
}
