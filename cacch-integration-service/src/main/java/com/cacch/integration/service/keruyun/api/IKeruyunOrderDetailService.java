package com.cacch.integration.service.keruyun.api;

import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderItemDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderPaymentDO;

import java.util.List;

/**
 * 客如云订单详情持久化服务接口
 *
 * <p>职责：待同步完结订单查询（主表）、单订单菜品/支付明细的幂等落库。
 * 落库策略为每单整体 delete + insert，与主表 detail_synced_at 更新在同一事务内完成。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunOrderDetailService {

    /**
     * 查询待同步详情的完结订单（finish_time 非空且 detail_synced_at 为空）
     *
     * <p>按完结时间升序返回，最多 limit 条。</p>
     *
     * @param limit 最大返回条数，必须大于 0
     * @return 待同步订单列表；无待同步数据时返回空列表（非 null）
     */
    List<KeruyunOrderDO> listOrdersPendingDetail(int limit);

    /**
     * 幂等落库单订单的菜品与支付明细
     *
     * <p>整体替换策略：先删除该订单已有的菜品/支付明细，再插入新数据，
     * 最后更新主表 detail_synced_at；三步在同一事务内完成，失败整体回滚。
     * 明细列表均可为空（如整单退款无支付明细），此时仅清空旧数据并标记已同步。</p>
     *
     * @param orderId  客如云订单 ID，不可为空
     * @param items    菜品明细列表（可为空列表）
     * @param payments 支付明细列表（可为空列表）
     */
    void saveOrderDetails(String orderId, List<KeruyunOrderItemDO> items, List<KeruyunOrderPaymentDO> payments);
}
