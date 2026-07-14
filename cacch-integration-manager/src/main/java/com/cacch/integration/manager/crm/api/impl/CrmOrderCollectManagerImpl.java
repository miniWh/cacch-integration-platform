package com.cacch.integration.manager.crm.api.impl;

import com.cacch.integration.common.config.crm.CrmCollectProperties;
import com.cacch.integration.common.constant.crm.CrmConstants;
import com.cacch.integration.common.dto.crm.CrmOrderCollectResult;
import com.cacch.integration.common.enums.crm.CrmDetailFetchStatusEnum;
import com.cacch.integration.common.enums.crm.CrmOaSyncStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.crm.CrmOrderDO;
import com.cacch.integration.entity.crm.CrmOrderDetailDO;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.support.CrmCollectTimeWindowSupport;
import com.cacch.integration.integration.crm.support.CrmOrderPayloadSupport;
import com.cacch.integration.manager.crm.api.ICrmOrderCollectManager;
import com.cacch.integration.service.crm.api.ICrmOpenApiService;
import com.cacch.integration.service.crm.api.ICrmOrderDetailService;
import com.cacch.integration.service.crm.api.ICrmOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * CRM 订单采集编排实现
 *
 * <p>按 modify_time 拉取订单（仅新增）→ 拉明细（仅新增）→ 补拉 PENDING/FAILED。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmOrderCollectManagerImpl implements ICrmOrderCollectManager {

    private final ICrmOpenApiService crmOpenApiService;
    private final ICrmOrderService crmOrderService;
    private final ICrmOrderDetailService crmOrderDetailService;
    private final CrmCollectProperties crmCollectProperties;

    @Override
    public CrmOrderCollectResult collectToday() {
        String[] window = CrmCollectTimeWindowSupport.todayWindowEpochMilli();
        log.info("【CrmCollect】开始当天采集, beginMilli={}, endMilli={}", window[0], window[1]);
        return collectInternal(window[0], window[1]);
    }

    @Override
    public CrmOrderCollectResult collectByModifyTime(String beginTime, String endTime) {
        if (!StringUtils.hasText(beginTime)) {
            log.info("【CrmCollect】采集终止, reason=beginTime为空");
            throw new BizException(
                    com.cacch.integration.common.result.ResultCode.PARAM_MISSING, "beginTime 不能为空");
        }
        String[] window = CrmCollectTimeWindowSupport.toQueryWindow(beginTime, endTime);
        log.info("【CrmCollect】开始指定时间窗采集, beginTime={}, endTime={}, beginMilli={}, endMilli={}",
                beginTime, endTime, window[0], window[1]);
        return collectInternal(window[0], window[1]);
    }

    private CrmOrderCollectResult collectInternal(String beginEpochMilli, String endEpochMilli) {
        int batchSize = Math.max(1, crmCollectProperties.getBatchSize());
        int detailPageSize = Math.max(1, crmCollectProperties.getDetailPageSize());

        int scanned = 0;
        int orderInserted = 0;
        int orderSkipped = 0;
        int detailFetchSuccess = 0;
        int detailFetchFailed = 0;
        int detailInserted = 0;
        int detailSkipped = 0;

        int page = CrmConstants.DEFAULT_PAGE;
        int remaining = batchSize;
        while (remaining > 0) {
            int rows = Math.min(remaining, detailPageSize);
            CrmOpenApiResponse response = crmOpenApiService.orderQueryByModifyTime(
                    page, rows, beginEpochMilli, endEpochMilli);
            List<JsonNode> records = CrmOrderPayloadSupport.listRecords(response.getResponseData());
            if (records.isEmpty()) {
                log.info("【CrmCollect】订单查询无更多数据, page={}", page);
                break;
            }
            for (JsonNode record : records) {
                if (remaining <= 0) {
                    break;
                }
                scanned++;
                remaining--;
                ProcessOrderOutcome outcome = processOneOrder(record, detailPageSize);
                orderInserted += outcome.orderInserted;
                orderSkipped += outcome.orderSkipped;
                detailFetchSuccess += outcome.detailFetchSuccess;
                detailFetchFailed += outcome.detailFetchFailed;
                detailInserted += outcome.detailInserted;
                detailSkipped += outcome.detailSkipped;
            }
            if (records.size() < rows) {
                break;
            }
            page++;
        }

        int retryLimit = crmCollectProperties.getDetailRetryBatchSize() > 0
                ? crmCollectProperties.getDetailRetryBatchSize()
                : batchSize;
        int retryScanned = 0;
        List<CrmOrderDO> needRetry = crmOrderService.listNeedDetailRetry(retryLimit);
        for (CrmOrderDO order : needRetry) {
            retryScanned++;
            DetailFetchOutcome detailOutcome = fetchAndPersistDetails(order, detailPageSize);
            detailFetchSuccess += detailOutcome.success ? 1 : 0;
            detailFetchFailed += detailOutcome.success ? 0 : 1;
            detailInserted += detailOutcome.inserted;
            detailSkipped += detailOutcome.skipped;
        }

        CrmOrderCollectResult result = CrmOrderCollectResult.builder()
                .beginEpochMilli(beginEpochMilli)
                .endEpochMilli(endEpochMilli)
                .scannedFromCrm(scanned)
                .orderInserted(orderInserted)
                .orderSkipped(orderSkipped)
                .detailFetchSuccess(detailFetchSuccess)
                .detailFetchFailed(detailFetchFailed)
                .detailInserted(detailInserted)
                .detailSkipped(detailSkipped)
                .retryScanned(retryScanned)
                .build();
        log.info("【CrmCollect】采集完成, scanned={}, inserted={}, skipped={}, detailOk={}, detailFail={}, "
                        + "detailInserted={}, detailSkipped={}, retryScanned={}",
                scanned, orderInserted, orderSkipped, detailFetchSuccess, detailFetchFailed,
                detailInserted, detailSkipped, retryScanned);
        return result;
    }

    private ProcessOrderOutcome processOneOrder(JsonNode record, int detailPageSize) {
        ProcessOrderOutcome outcome = new ProcessOrderOutcome();
        String orderNo = CrmOrderPayloadSupport.text(record, "name");
        String crmOrderId = CrmOrderPayloadSupport.text(record, "id");
        if (!StringUtils.hasText(orderNo) || !StringUtils.hasText(crmOrderId)) {
            log.info("【CrmCollect】跳过订单, reason=缺少name或id");
            outcome.orderSkipped = 1;
            return outcome;
        }

        CrmOrderDO existing = crmOrderService.getByOrderNo(orderNo);
        CrmOrderDO order;
        if (existing != null) {
            log.info("【CrmCollect】订单已存在跳过主表写入, orderNo={}, crmOrderId={}", orderNo, crmOrderId);
            outcome.orderSkipped = 1;
            order = existing;
        } else {
            try {
                order = crmOrderService.insert(buildOrder(record, orderNo, crmOrderId));
                outcome.orderInserted = 1;
            } catch (DataIntegrityViolationException e) {
                log.info("【CrmCollect】订单并发写入冲突，按已存在处理, orderNo={}, reason={}",
                        orderNo, e.getMessage());
                order = crmOrderService.getByOrderNo(orderNo);
                outcome.orderSkipped = 1;
                if (order == null) {
                    log.info("【CrmCollect】冲突后仍查无订单，跳过明细, orderNo={}", orderNo);
                    return outcome;
                }
            }
        }

        if (CrmDetailFetchStatusEnum.SUCCESS.getCode().equals(order.getDetailFetchStatus())) {
            log.info("【CrmCollect】明细已成功，跳过拉取, orderNo={}, orderId={}", orderNo, order.getId());
            return outcome;
        }

        DetailFetchOutcome detailOutcome = fetchAndPersistDetails(order, detailPageSize);
        if (detailOutcome.success) {
            outcome.detailFetchSuccess = 1;
        } else {
            outcome.detailFetchFailed = 1;
        }
        outcome.detailInserted = detailOutcome.inserted;
        outcome.detailSkipped = detailOutcome.skipped;
        return outcome;
    }

    private DetailFetchOutcome fetchAndPersistDetails(CrmOrderDO order, int detailPageSize) {
        DetailFetchOutcome outcome = new DetailFetchOutcome();
        try {
            int page = CrmConstants.DEFAULT_PAGE;
            while (true) {
                CrmOpenApiResponse response = crmOpenApiService.orderDetailQueryByOrderId(
                        page, detailPageSize, order.getCrmOrderId());
                List<JsonNode> details = CrmOrderPayloadSupport.listRecords(response.getResponseData());
                if (details.isEmpty()) {
                    break;
                }
                for (JsonNode detailNode : details) {
                    String crmDetailId = CrmOrderPayloadSupport.text(detailNode, "id");
                    if (!StringUtils.hasText(crmDetailId)) {
                        log.info("【CrmCollect】跳过明细, orderNo={}, reason=明细id为空", order.getOrderNo());
                        outcome.skipped++;
                        continue;
                    }
                    if (crmOrderDetailService.existsByCrmDetailId(crmDetailId)) {
                        outcome.skipped++;
                        continue;
                    }
                    try {
                        crmOrderDetailService.insert(buildDetail(order, detailNode, crmDetailId));
                        outcome.inserted++;
                    } catch (DataIntegrityViolationException e) {
                        log.info("【CrmCollect】明细并发冲突按已存在处理, crmDetailId={}", crmDetailId);
                        outcome.skipped++;
                    }
                }
                if (details.size() < detailPageSize) {
                    break;
                }
                page++;
            }
            crmOrderService.updateDetailFetchStatus(
                    order.getId(), CrmDetailFetchStatusEnum.SUCCESS.getCode(), null);
            outcome.success = true;
            log.info("【CrmCollect】明细拉取成功, orderNo={}, inserted={}, skipped={}",
                    order.getOrderNo(), outcome.inserted, outcome.skipped);
        } catch (Exception e) {
            outcome.success = false;
            String msg = e.getMessage();
            log.info("【CrmCollect】明细拉取失败不回滚主表, orderNo={}, reason={}", order.getOrderNo(), msg);
            log.error("【CrmCollect】明细拉取失败, orderNo={}, crmOrderId={}",
                    order.getOrderNo(), order.getCrmOrderId(), e);
            crmOrderService.updateDetailFetchStatus(
                    order.getId(), CrmDetailFetchStatusEnum.FAILED.getCode(),
                    msg == null ? "明细拉取失败" : msg);
        }
        return outcome;
    }

    private CrmOrderDO buildOrder(JsonNode record, String orderNo, String crmOrderId) {
        CrmOrderDO order = new CrmOrderDO();
        order.setCrmOrderId(crmOrderId);
        order.setOrderNo(orderNo);
        order.setCustomerId(CrmOrderPayloadSupport.nestedText(record, "customer", "id"));
        order.setCustomerName(CrmOrderPayloadSupport.nestedText(record, "customer", "name"));
        order.setOwnerId(CrmOrderPayloadSupport.nestedText(record, "owner", "id"));
        order.setOwnerName(CrmOrderPayloadSupport.nestedText(record, "owner", "name"));
        order.setCurrencyType(CrmOrderPayloadSupport.nestedText(record, "currency_type", "label"));
        if (!StringUtils.hasText(order.getCurrencyType())) {
            order.setCurrencyType(CrmOrderPayloadSupport.text(record, "currency_type"));
        }
        order.setOrderTotalAmount(CrmOrderPayloadSupport.text(record, "order_total_amount"));
        order.setCrmCreateTime(CrmOrderPayloadSupport.parseCrmDateTime(record.get("create_time")));
        order.setCrmModifyTime(CrmOrderPayloadSupport.parseCrmDateTime(record.get("modify_time")));
        order.setDetailFetchStatus(CrmDetailFetchStatusEnum.PENDING.getCode());
        order.setRawPayload(CrmOrderPayloadSupport.toJsonObject(record));
        return order;
    }

    private CrmOrderDetailDO buildDetail(CrmOrderDO order, JsonNode detailNode, String crmDetailId) {
        CrmOrderDetailDO detail = new CrmOrderDetailDO();
        detail.setOrderId(order.getId());
        detail.setCrmOrderId(order.getCrmOrderId());
        detail.setOrderNo(order.getOrderNo());
        detail.setCrmDetailId(crmDetailId);
        detail.setDetailName(CrmOrderPayloadSupport.text(detailNode, "name"));
        detail.setPdCode(CrmOrderPayloadSupport.text(detailNode, "pd_code"));
        detail.setPdCount(CrmOrderPayloadSupport.text(detailNode, "pd_count"));
        detail.setActualPrice(CrmOrderPayloadSupport.text(detailNode, "actual_price"));
        String material = CrmOrderPayloadSupport.text(detailNode, "field_Mb25P__c");
        if (!StringUtils.hasText(material)) {
            material = CrmOrderPayloadSupport.nestedText(detailNode, "field_Mb25P__c", "name");
        }
        detail.setMaterialCode(material);
        detail.setOaSyncStatus(CrmOaSyncStatusEnum.PENDING.getCode());
        detail.setRetryCount(0);
        detail.setRawPayload(CrmOrderPayloadSupport.toJsonObject(detailNode));
        return detail;
    }

    private static final class ProcessOrderOutcome {
        private int orderInserted;
        private int orderSkipped;
        private int detailFetchSuccess;
        private int detailFetchFailed;
        private int detailInserted;
        private int detailSkipped;
    }

    private static final class DetailFetchOutcome {
        private boolean success;
        private int inserted;
        private int skipped;
    }
}
