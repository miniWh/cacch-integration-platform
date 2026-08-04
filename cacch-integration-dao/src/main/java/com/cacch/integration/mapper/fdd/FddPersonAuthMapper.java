package com.cacch.integration.mapper.fdd;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.fdd.FddPersonAuthDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 法大大个人实名认证记录 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface FddPersonAuthMapper extends BaseMapper<FddPersonAuthDO> {
}
