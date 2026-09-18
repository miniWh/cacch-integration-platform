package com.cacch.integration.dto.moka.vo;

import lombok.Data;

/**
 * Moka 组织架构全量同步结果视图对象
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptSyncResultVO {

    /**
     * 新增部门数量
     */
    private Integer newCount;

    /**
     * 更新部门数量
     */
    private Integer updateCount;

    /**
     * 标记删除部门数量
     */
    private Integer deleteCount;
}
