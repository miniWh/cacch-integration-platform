package com.cacch.integration.convert.keruyun;

import com.cacch.integration.dto.keruyun.vo.KeruyunOrderSyncTaskVO;
import com.cacch.integration.entity.keruyun.KeruyunOrderSyncTaskDO;
import org.mapstruct.Mapper;

/**
 * 客如云订单同步任务 DO → VO 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface KeruyunOrderSyncTaskConverter {

    /**
     * DO → VO
     *
     * @param source 控制表 DO
     * @return 任务视图对象
     */
    KeruyunOrderSyncTaskVO toVO(KeruyunOrderSyncTaskDO source);
}
