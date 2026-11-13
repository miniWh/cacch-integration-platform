package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.MdmDeptDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门主数据主档 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MdmDeptMapper extends BaseMapper<MdmDeptDO> {
}
