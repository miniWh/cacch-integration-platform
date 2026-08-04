package com.cacch.integration.convert.fdd;

import com.cacch.integration.common.dto.fdd.FddAuthQueryResult;
import com.cacch.integration.dto.fdd.vo.FddAuthQueryVO;
import org.mapstruct.Mapper;

/**
 * 法大大认证结果 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface FddAuthConverter {

    /**
     * 查询结果转 VO
     *
     * @param result 编排层结果
     * @return 对外 VO
     */
    FddAuthQueryVO toVO(FddAuthQueryResult result);
}
