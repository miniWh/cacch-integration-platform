package com.cacch.integration.mapper.heart;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.heart.HeartPoDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Heart 客户 PO 订单数据 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface HeartPoMapper extends BaseMapper<HeartPoDO> {
}
