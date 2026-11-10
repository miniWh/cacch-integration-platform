package com.cacch.integration.convert.ihr;

import com.cacch.integration.dto.ihr.vo.PersondetailVO;
import com.cacch.integration.entity.ihr.PersondetailDO;
import org.mapstruct.Mapper;

/**
 * iHR 人员 MapStruct 转换器
 *
 * <p>负责 persondetail 实体到 web 视图对象的转换，字段名称一一对应。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface IhrPersonConverter {

    /**
     * persondetail 实体 → 人员详情视图
     *
     * @param source persondetail 实体
     * @return 人员详情视图；source 为 null 时返回 null
     */
    PersondetailVO toVO(PersondetailDO source);
}
