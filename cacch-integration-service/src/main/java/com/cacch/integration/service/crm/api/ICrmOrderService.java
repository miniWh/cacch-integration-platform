package com.cacch.integration.service.crm.api;

import com.cacch.integration.entity.crm.CrmOrderDO;

import java.util.List;

/**
 * CRM 订单持久化服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmOrderService {

    /**
     * 按订单编号查询
     *
     * @param orderNo CRM 订单编号（name）
     * @return 订单；不存在返回 null
     */
    CrmOrderDO getByOrderNo(String orderNo);

    /**
     * 按 CRM 订单内部 ID 查询
     *
     * @param crmOrderId CRM 订单 id
     * @return 订单；不存在返回 null
     */
    CrmOrderDO getByCrmOrderId(String crmOrderId);

    /**
     * 新增订单（仅新增）
     *
     * @param order 订单实体
     * @return 插入后实体
     */
    CrmOrderDO insert(CrmOrderDO order);

    /**
     * 更新明细拉取状态
     *
     * @param orderId 主键
     * @param status  状态码
     * @param error   错误信息，成功时可为 null
     */
    void updateDetailFetchStatus(Long orderId, String status, String error);

    /**
     * 查询待补拉明细的订单（PENDING / FAILED）
     *
     * @param limit 最大条数
     * @return 订单列表；无数据返回空列表
     */
    List<CrmOrderDO> listNeedDetailRetry(int limit);
}
