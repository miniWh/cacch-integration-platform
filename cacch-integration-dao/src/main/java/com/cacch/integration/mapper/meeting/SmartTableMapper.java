package com.cacch.integration.mapper.meeting;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.meeting.SmartTableDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能表格配置 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface SmartTableMapper extends BaseMapper<SmartTableDO> {
}
