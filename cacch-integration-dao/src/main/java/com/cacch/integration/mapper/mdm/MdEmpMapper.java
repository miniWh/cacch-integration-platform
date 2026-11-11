package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.MdEmpDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人员主数据身份主档 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MdEmpMapper extends BaseMapper<MdEmpDO> {
}
