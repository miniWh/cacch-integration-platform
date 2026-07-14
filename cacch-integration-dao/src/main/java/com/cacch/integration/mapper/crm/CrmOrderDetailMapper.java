package com.cacch.integration.mapper.crm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.crm.CrmOrderDetailDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CRM 订单明细 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface CrmOrderDetailMapper extends BaseMapper<CrmOrderDetailDO> {
}
