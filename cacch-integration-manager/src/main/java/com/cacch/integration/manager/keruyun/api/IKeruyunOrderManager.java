package com.cacch.integration.manager.keruyun.api;

import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncResult;

/**
 * 客如云订单同步 Manager 接口
 *
 * <p>同步链路：参数校验 → 取 token → 分页查询 → 逐条落库（单条失败不阻断）。
 * 两种调用方式：手动透传查询（syncOrders）、按任务驱动（syncByTaskId）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunOrderManager {

    /**
     * 手动指定时间窗口同步（调试/快速验证入口，不走控制表）
     *
     * @param dateType        查询时间类型
     * @param startDate       查询开始时间
     * @param endDate         查询结束时间
     * @param orderTypeList   订单类型过滤（可为 null）
     * @param orderStatusList 订单状态过滤（可为 null）
     * @param pageSize        每页条数（null 时默认 50，超 100 截断）
     * @return 同步统计结果
     */
    KeruyunOrderSyncResult syncOrders(String dateType, String startDate, String endDate,
                                      java.util.List<String> orderTypeList,
                                      java.util.List<String> orderStatusList,
                                      Integer pageSize);

    /**
     * 按任务驱动同步（控制表入口，支持断点续传）
     *
     * @param taskId 任务主键
     */
    void syncByTaskId(Long taskId);
}
