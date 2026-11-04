package com.cacch.integration.mapper.keruyun;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.keruyun.KeruyunOrderPaymentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客如云订单支付明细落库 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface KeruyunOrderPaymentMapper extends BaseMapper<KeruyunOrderPaymentDO> {
}
