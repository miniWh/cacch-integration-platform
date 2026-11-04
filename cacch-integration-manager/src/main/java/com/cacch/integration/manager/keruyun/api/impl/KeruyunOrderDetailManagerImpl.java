package com.cacch.integration.manager.keruyun.api.impl;

import com.cacch.integration.common.constant.keruyun.KeruyunOrderDetailConstants;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderDetailSyncItem;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderDetailSyncResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderItemDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderPaymentDO;
import com.cacch.integration.integration.keruyun.client.KeruyunOrderClient;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderDetailResponse;
import com.cacch.integration.manager.keruyun.api.IKeruyunManager;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderDetailManager;
import com.cacch.integration.manager.keruyun.convert.KeruyunOrderDetailConverter;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderDetailService;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 客如云订单详情同步编排实现
 *
 * <p>流程：筛选完结订单（finish_time 非空且 detail_synced_at 为空）→ 取 token →
 * 固定并发拉取详情 → 菜品/支付明细幂等落库。单订单失败不阻断整批，
 * 失败订单因 detail_synced_at 未更新，下次触发时自动重试。</p>
 *
 * <p>并发控制：使用固定大小线程池（见 web 层 keruyunOrderDetailSyncExecutor），
 * 峰值调用量受并发数约束，远低于客如云接口 220 QPS 限流阈值。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
public class KeruyunOrderDetailManagerImpl implements IKeruyunOrderDetailManager {

    private static final String BIZ = "客如云订单详情同步";

    private final IKeruyunManager keruyunManager;
    private final KeruyunOrderClient keruyunOrderClient;
    private final IKeruyunOrderService keruyunOrderService;
    private final IKeruyunOrderDetailService detailService;
    private final KeruyunOrderDetailConverter detailConverter;
    private final Executor detailSyncExecutor;

    /**
     * 构造注入（线程池须按 Bean 名限定，显式声明构造器）
     *
     * @param keruyunManager      token 获取入口
     * @param keruyunOrderClient  订单详情客户端
     * @param keruyunOrderService 订单查询服务
     * @param detailService       详情持久化服务
     * @param detailConverter     详情转换器
     * @param detailSyncExecutor  详情同步固定并发线程池
     */
    public KeruyunOrderDetailManagerImpl(IKeruyunManager keruyunManager,
                                         KeruyunOrderClient keruyunOrderClient,
                                         IKeruyunOrderService keruyunOrderService,
                                         IKeruyunOrderDetailService detailService,
                                         KeruyunOrderDetailConverter detailConverter,
                                         @Qualifier("keruyunOrderDetailSyncExecutor") Executor detailSyncExecutor) {
        this.keruyunManager = keruyunManager;
        this.keruyunOrderClient = keruyunOrderClient;
        this.keruyunOrderService = keruyunOrderService;
        this.detailService = detailService;
        this.detailConverter = detailConverter;
        this.detailSyncExecutor = detailSyncExecutor;
    }

    @Override
    public KeruyunOrderDetailSyncResult syncOrderDetails(Integer limit) {
        int effectiveLimit = resolveLimit(limit);
        KeruyunOrderDetailSyncResult result = new KeruyunOrderDetailSyncResult(effectiveLimit);

        // 仅取完结（finish_time 非空）且详情未同步的订单
        List<KeruyunOrderDO> orders = detailService.listOrdersPendingDetail(effectiveLimit);
        result.setPendingTotal(orders.size());
        if (orders.isEmpty()) {
            log.info("【{}】批量同步终止, reason=无待同步完结订单, limit={}", BIZ, effectiveLimit);
            return result;
        }

        String token = keruyunManager.getAccessToken();
        log.info("【{}】批量同步开始, limit={}, concurrency={}, orderCount={}",
                BIZ, effectiveLimit, KeruyunOrderDetailConstants.DETAIL_SYNC_CONCURRENCY, orders.size());

        // 固定并发拉取：每个订单一个任务，结果在主线程聚合（避免并发写结果对象）
        List<CompletableFuture<DetailOutcome>> futures = orders.stream()
                .map(order -> CompletableFuture.supplyAsync(
                        () -> processOne(order.getOrderId(), token), detailSyncExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (CompletableFuture<DetailOutcome> future : futures) {
            DetailOutcome outcome = future.join();
            if (outcome.error() == null) {
                outcome.item().setItemCount(outcome.itemCount());
                outcome.item().setPaymentCount(outcome.paymentCount());
                result.recordSuccess(outcome.item());
            } else {
                result.recordFailure(outcome.item(), outcome.error());
            }
        }

        log.info("【{}】批量同步完成, pendingTotal={}, limit={}, successCount={}, failureCount={}, skipCount={}",
                BIZ, result.getPendingTotal(), result.getLimit(), result.getSuccessCount(),
                result.getFailureCount(), result.getSkipCount());
        return result;
    }

    @Override
    public KeruyunOrderDetailSyncResult syncOrderDetail(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            log.info("【{}】单条同步终止, reason=订单ID为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "orderId 不能为空");
        }
        KeruyunOrderDetailSyncResult result =
                new KeruyunOrderDetailSyncResult(KeruyunOrderDetailConstants.DETAIL_SYNC_BATCH_DEFAULT);

        KeruyunOrderDO order = keruyunOrderService.getByOrderId(orderId);
        if (order == null) {
            log.info("【{}】单条同步终止, reason=订单不存在, orderId={}", BIZ, orderId);
            throw new BizException(ResultCode.PARAM_INVALID, "客如云订单不存在: " + orderId);
        }
        if (order.getFinishTime() == null) {
            log.info("【{}】单条同步跳过, reason=订单未完结, orderId={}, orderStatus={}",
                    BIZ, orderId, order.getOrderStatus());
            result.recordSkip(buildItem(order), "订单未完结，不拉取详情");
            return result;
        }

        String token = keruyunManager.getAccessToken();
        DetailOutcome outcome = processOne(orderId, token);
        if (outcome.error() == null) {
            outcome.item().setItemCount(outcome.itemCount());
            outcome.item().setPaymentCount(outcome.paymentCount());
            result.recordSuccess(outcome.item());
        } else {
            result.recordFailure(outcome.item(), outcome.error());
        }

        log.info("【{}】单条同步完成, orderId={}, successCount={}, failureCount={}, skipCount={}",
                BIZ, orderId, result.getSuccessCount(), result.getFailureCount(), result.getSkipCount());
        return result;
    }

    /**
     * 处理单个订单的详情拉取与落库（工作线程内执行）
     *
     * @param orderId 客如云订单 ID
     * @param token   门店 token
     * @return 处理结果（error 为 null 表示成功）
     */
    private DetailOutcome processOne(String orderId, String token) {
        KeruyunOrderDO order = keruyunOrderService.getByOrderId(orderId);
        KeruyunOrderDetailSyncItem item = buildItem(order);
        try {
            KeruyunOrderDetailResponse response = keruyunOrderClient.queryOrderDetail(orderId, token);
            KeruyunOrderDetailResponse.DataInfo data =
                    response.getResult() == null ? null : response.getResult().getData();
            if (data == null) {
                log.info("【{}】单条处理终止, orderId={}, reason=详情数据为空", BIZ, orderId);
                return new DetailOutcome(item, 0, 0, "详情数据为空");
            }

            List<KeruyunOrderItemDO> items = new ArrayList<>();
            flattenItems(data.getOrderItemVoList(), orderId, items);

            List<KeruyunOrderPaymentDO> payments = new ArrayList<>();
            if (data.getOpenPaymentDetailVoList() != null) {
                for (KeruyunOrderDetailResponse.PaymentDetailVo vo : data.getOpenPaymentDetailVoList()) {
                    KeruyunOrderPaymentDO paymentDO = detailConverter.toPaymentDO(vo);
                    paymentDO.setOrderId(orderId);
                    payments.add(paymentDO);
                }
            }

            detailService.saveOrderDetails(orderId, items, payments);
            return new DetailOutcome(item, items.size(), payments.size(), null);
        } catch (Exception e) {
            log.info("【{}】单条处理终止, orderId={}, reason={}", BIZ, orderId, e.getMessage());
            log.error("【{}】单条详情同步失败, orderId={}", BIZ, orderId, e);
            return new DetailOutcome(item, 0, 0, e.getMessage());
        }
    }

    /**
     * 递归平铺菜品明细（套餐子商品 children 随父行存入同一表，父行 ID 由 parentId 关联）
     */
    private void flattenItems(List<KeruyunOrderDetailResponse.OrderItemVo> vos, String orderId,
                              List<KeruyunOrderItemDO> out) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        for (KeruyunOrderDetailResponse.OrderItemVo vo : vos) {
            KeruyunOrderItemDO itemDO = detailConverter.toItemDO(vo);
            itemDO.setOrderId(orderId);
            out.add(itemDO);
            flattenItems(vo.getChildren(), orderId, out);
        }
    }

    /**
     * 构建明细主体键信息
     */
    private KeruyunOrderDetailSyncItem buildItem(KeruyunOrderDO order) {
        KeruyunOrderDetailSyncItem item = new KeruyunOrderDetailSyncItem();
        if (order != null) {
            item.setOrderId(order.getOrderId());
            item.setBusiOrderNo(order.getBusiOrderNo());
            item.setShopId(order.getShopId());
        }
        return item;
    }

    /**
     * 解析单次处理上限：null 取默认值，超上限截断
     */
    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return KeruyunOrderDetailConstants.DETAIL_SYNC_BATCH_DEFAULT;
        }
        return Math.min(limit, KeruyunOrderDetailConstants.DETAIL_SYNC_BATCH_MAX);
    }

    /**
     * 单订单处理结果（工作线程返回值，主线程聚合）
     *
     * @param item         明细主体
     * @param itemCount    落库菜品条数（成功时有意义）
     * @param paymentCount 落库支付条数（成功时有意义）
     * @param error        失败原因；null 表示成功
     */
    private record DetailOutcome(KeruyunOrderDetailSyncItem item, int itemCount, int paymentCount, String error) {
    }
}
