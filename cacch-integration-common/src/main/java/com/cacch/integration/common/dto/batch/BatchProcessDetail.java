package com.cacch.integration.common.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 批量处理明细
 *
 * @param <T> 明细主体类型，由各业务域定义（如订单、人员、部门）
 * @author hongfu_zhou@cacch.com
 */
@Data
@AllArgsConstructor
public class BatchProcessDetail<T> {

    /**
     * 明细主体，承载业务定位所需的键信息
     */
    private T item;

    /**
     * 处理结果状态
     */
    private BatchProcessStatus status;

    /**
     * 失败或跳过原因；成功时为 null
     */
    private String reason;
}
