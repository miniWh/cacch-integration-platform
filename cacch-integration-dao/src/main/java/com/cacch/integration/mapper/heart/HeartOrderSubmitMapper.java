package com.cacch.integration.mapper.heart;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.heart.HeartOrderSubmitDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Heart 客户 PO 提交记录 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface HeartOrderSubmitMapper extends BaseMapper<HeartOrderSubmitDO> {
}
