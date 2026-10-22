package com.cacch.integration.mapper.keruyun;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客如云订单落库 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface KeruyunOrderMapper extends BaseMapper<KeruyunOrderDO> {
}
