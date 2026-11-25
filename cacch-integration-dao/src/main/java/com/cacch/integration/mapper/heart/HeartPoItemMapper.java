package com.cacch.integration.mapper.heart;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.heart.HeartPoItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Heart 客户 PO 明细行 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface HeartPoItemMapper extends BaseMapper<HeartPoItemDO> {
}
