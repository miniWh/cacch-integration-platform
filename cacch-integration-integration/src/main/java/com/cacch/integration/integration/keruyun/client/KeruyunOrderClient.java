package com.cacch.integration.integration.keruyun.client;

import com.cacch.integration.common.constant.keruyun.KeruyunCommonConstants;
import com.cacch.integration.common.constant.keruyun.KeruyunPathConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderDetailQueryRequest;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderDetailResponse;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderQueryRequest;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 客如云订单接口客户端
 *
 * <p>接口：{@code POST /open/standard/order/queryList}。签名、URL 组装与出入参日志
 * 由 {@link KeruyunClient} 统一承担；本类只做单页请求与业务成功码校验。</p>
 *
 * <p>客如云文档明确禁止轮询调用，本接口仅用于按时间窗手动触发的一次性拉取。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeruyunOrderClient {

    private static final String ACTION = "查询订单列表";

    private final KeruyunClient keruyunClient;

    /**
     * 查询单页订单列表
     *
     * @param request 订单查询请求体，含时间窗与分页参数，不可为空
     * @param token   门店 token，由 Token Service 提供，不可为空
     * @return 客如云订单查询响应，已校验 {@code code=0}
     * @throws BizException 响应成功码非 0 时抛出
     */
    public KeruyunOrderQueryResponse queryOrderList(KeruyunOrderQueryRequest request, String token) {
        log.debug("【客如云签名】订单查询请求, dateType={}, startDate={}, endDate={}, "
                        + "orderTypeList={}, orderStatusList={}, pageBean={}",
                request.getDateType(), request.getStartDate(), request.getEndDate(),
                request.getOrderTypeList(), request.getOrderStatusList(), request.getPageBean());
        KeruyunOrderQueryResponse response = keruyunClient.post(
                KeruyunPathConstants.PATH_QUERY_ORDER_LIST, request, token, KeruyunOrderQueryResponse.class);
        if (response == null || !response.isSuccess()) {
            String code = response == null ? "null" : String.valueOf(response.getCode());
            String message = response == null ? "响应为空" : response.getMessage();
            log.info("【{}】{}终止, code={}, message={}, pageNum={}, pageSize={}",
                    KeruyunCommonConstants.LOG_BIZ, ACTION, code, message,
                    request.getPageBean() == null ? null : request.getPageBean().getPageNum(),
                    request.getPageBean() == null ? null : request.getPageBean().getPageSize());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "客如云订单查询失败: code=" + code + ", message=" + message);
        }
        String totalCount = response.getResult() == null || response.getResult().getData() == null
                ? null : response.getResult().getData().getTotalCount();
        log.info("【{}】{}成功, pageNum={}, pageSize={}, totalCount={}, messageUuid={}",
                KeruyunCommonConstants.LOG_BIZ, ACTION,
                request.getPageBean() == null ? null : request.getPageBean().getPageNum(),
                request.getPageBean() == null ? null : request.getPageBean().getPageSize(),
                totalCount, response.getMessageUuid());
        return response;
    }

    /**
     * 查询单条订单详情
     *
     * <p>对应接口 {@code POST /open/standard/order/queryDetail}（最大 QPS 220/秒），
     * 仅用于完结订单的明细拉取，禁止轮询。</p>
     *
     * @param orderId 客如云订单 ID，不可为空
     * @param token   门店 token，由 Token Service 提供，不可为空
     * @return 订单详情响应（含菜品明细与支付明细），已校验 {@code code=0}
     * @throws BizException 响应成功码非 0 时抛出
     */
    public KeruyunOrderDetailResponse queryOrderDetail(String orderId, String token) {
        KeruyunOrderDetailQueryRequest request = new KeruyunOrderDetailQueryRequest(orderId);
        KeruyunOrderDetailResponse response = keruyunClient.post(
                KeruyunPathConstants.PATH_QUERY_ORDER_DETAIL, request, token, KeruyunOrderDetailResponse.class);
        if (response == null || !response.isSuccess()) {
            String code = response == null ? "null" : String.valueOf(response.getCode());
            String message = response == null ? "响应为空" : response.getMessage();
            log.info("【{}】查询订单详情终止, orderId={}, code={}, message={}",
                    KeruyunCommonConstants.LOG_BIZ, orderId, code, message);
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "客如云订单详情查询失败: code=" + code + ", message=" + message);
        }
        int itemCount = response.getResult() == null || response.getResult().getData() == null
                || response.getResult().getData().getOrderItemVoList() == null
                ? 0 : response.getResult().getData().getOrderItemVoList().size();
        int paymentCount = response.getResult() == null || response.getResult().getData() == null
                || response.getResult().getData().getOpenPaymentDetailVoList() == null
                ? 0 : response.getResult().getData().getOpenPaymentDetailVoList().size();
        log.info("【{}】查询订单详情成功, orderId={}, itemCount={}, paymentCount={}, messageUuid={}",
                KeruyunCommonConstants.LOG_BIZ, orderId, itemCount, paymentCount, response.getMessageUuid());
        return response;
    }
}
