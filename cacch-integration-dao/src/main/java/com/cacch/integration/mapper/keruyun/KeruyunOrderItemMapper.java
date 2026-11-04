package com.cacch.integration.mapper.keruyun;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.keruyun.KeruyunOrderItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客如云订单菜品明细落库 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface KeruyunOrderItemMapper extends BaseMapper<KeruyunOrderItemDO> {
}
