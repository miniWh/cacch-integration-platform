package com.cacch.integration.mapper.heart;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.heart.HeartMailDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Heart 来源邮件数据 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface HeartMailMapper extends BaseMapper<HeartMailDO> {
}
