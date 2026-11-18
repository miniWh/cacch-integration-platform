package com.cacch.integration.convert.crm;

import com.cacch.integration.common.dto.crm.CrmEmployeeDTO;
import com.cacch.integration.dto.crm.vo.CrmEmployeeVO;
import org.mapstruct.Mapper;

/**
 * 勤策员工 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface CrmEmployeeConverter {

    /**
     * 服务层 DTO 转为对外 VO
     *
     * @param dto 员工 DTO，可空
     * @return 员工 VO；dto 为 null 时返回 null
     */
    CrmEmployeeVO toVO(CrmEmployeeDTO dto);
}