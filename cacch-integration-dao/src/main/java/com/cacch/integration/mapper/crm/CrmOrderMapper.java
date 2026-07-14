package com.cacch.integration.mapper.crm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.crm.CrmOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CRM 订单主表 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface CrmOrderMapper extends BaseMapper<CrmOrderDO> {
}
