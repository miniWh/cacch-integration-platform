package com.cacch.integration.manager.keruyun.api.impl;

import com.cacch.integration.common.constant.keruyun.KeruyunOrderConstants;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.integration.keruyun.client.KeruyunOrderClient;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderItem;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderQueryRequest;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderQueryResponse;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunPageBean;
import com.cacch.integration.manager.keruyun.api.IKeruyunManager;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderManager;
import com.cacch.integration.manager.keruyun.convert.KeruyunOrderConverter;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * 客如云订单同步编排实现
 *
 * <p>流程：参数校验 → 取门店 token → 逐页查询 → 逐条落库（单条失败不阻断）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeruyunOrderManagerImpl implements IKeruyunOrderManager {

    private static final String BIZ = "客如云订单同步";

    /**
     * 允许的查询时间类型
     */
    private static final Set<String> ALLOWED_DATE_TYPES = Set.of(
            KeruyunOrderConstants.DATE_TYPE_FINISH_BUSI_DATE, "OPEN_TIME", "FINISH_TIME", "SETTLE_TIME");

    private static final DateTimeFormatter SECOND_FORMATTER =
            DateTimeFormatter.ofPattern(KeruyunOrderConstants.DATE_PATTERN_SECOND);

    private static final DateTimeFormatter DAY_FORMATTER =
            DateTimeFormatter.ofPattern(KeruyunOrderConstants.DATE_PATTERN_DAY);

    private final IKeruyunManager keruyunManager;
    private final KeruyunOrderClient keruyunOrderClient;
    private final IKeruyunOrderService keruyunOrderService;
    private final KeruyunOrderConverter keruyunOrderConverter;

    @Override
    public KeruyunOrderSyncResult syncOrders(String dateType, String startDate, String endDate,
                                             List<String> orderTypeList, List<String> orderStatusList,
                                             Integer pageSize) {
        validateWindow(dateType, startDate, endDate);

        int size = resolvePageSize(pageSize);
        String syncBatch = dateType + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String token = keruyunManager.getAccessToken();

        KeruyunOrderQueryRequest request = new KeruyunOrderQueryRequest();
        request.setDateType(dateType);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setOrderTypeList(orderTypeList);
        request.setOrderStatusList(orderStatusList);

        int pageNum = 1;
        int pageCount = 0;
        int scanned = 0;
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        while (pageNum <= KeruyunOrderConstants.ORDER_MAX_PAGE_GUARD) {
            request.setPageBean(new KeruyunPageBean(String.valueOf(pageNum), String.valueOf(size)));
            KeruyunOrderQueryResponse response = keruyunOrderClient.queryOrderList(request, token);
            KeruyunOrderQueryResponse.DataInfo data =
                    response.getResult() == null ? null : response.getResult().getData();
            if (data == null || data.getList() == null || data.getList().isEmpty()) {
                log.info("【{}】查询无更多数据, dateType={}, pageNum={}, reason=列表为空",
                        BIZ, dateType, pageNum);
                break;
            }

            pageCount++;
            for (KeruyunOrderItem item : data.getList()) {
                scanned++;
                if (!StringUtils.hasText(item.getOrderId())) {
                    skipped++;
                    log.info("【{}】跳过订单, reason=orderId为空, pageNum={}, busiOrderNo={}",
                            BIZ, pageNum, item.getBusiOrderNo());
                    continue;
                }
                try {
                    KeruyunOrderDO orderDO = keruyunOrderConverter.toDO(item);
                    orderDO.setSyncBatch(syncBatch);
                    orderDO.setSyncedAt(LocalDateTime.now());
                    if (keruyunOrderService.saveOrUpdate(orderDO)) {
                        inserted++;
                    } else {
                        updated++;
                    }
                } catch (Exception e) {
                    failed++;
                    log.info("【{}】单条落库终止, orderId={}, pageNum={}, reason={}",
                            BIZ, item.getOrderId(), pageNum, e.getMessage());
                    log.error("【{}】单条落库失败, orderId={}, pageNum={}",
                            BIZ, item.getOrderId(), pageNum, e);
                }
            }

            int totalPage = parseIntOrDefault(data.getTotalPage(), pageNum);
            if (pageNum >= totalPage) {
                break;
            }
            pageNum++;
        }

        if (pageNum > KeruyunOrderConstants.ORDER_MAX_PAGE_GUARD) {
            log.warn("【{}】翻页达到保护上限提前终止, dateType={}, maxPage={}",
                    BIZ, dateType, KeruyunOrderConstants.ORDER_MAX_PAGE_GUARD);
        }

        KeruyunOrderSyncResult result = KeruyunOrderSyncResult.builder()
                .dateType(dateType)
                .startDate(startDate)
                .endDate(endDate)
                .pageCount(pageCount)
                .scanned(scanned)
                .inserted(inserted)
                .updated(updated)
                .skipped(skipped)
                .failed(failed)
                .build();
        log.info("【{}】同步完成, dateType={}, startDate={}, endDate={}, pageCount={}, scanned={}, "
                        + "inserted={}, updated={}, skipped={}, failed={}",
                BIZ, dateType, startDate, endDate, pageCount, scanned, inserted, updated, skipped, failed);
        return result;
    }

    /**
     * 校验查询时间类型、时间格式与最大跨度（31 天）
     */
    private void validateWindow(String dateType, String startDate, String endDate) {
        if (!StringUtils.hasText(dateType)) {
            log.info("【{}】同步终止, reason=dateType为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "dateType 不能为空");
        }
        if (!ALLOWED_DATE_TYPES.contains(dateType)) {
            log.info("【{}】同步终止, reason=dateType非法, dateType={}", BIZ, dateType);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "dateType 仅支持 FINISH_BUSI_DATE / OPEN_TIME / FINISH_TIME / SETTLE_TIME");
        }
        if (!StringUtils.hasText(startDate) || !StringUtils.hasText(endDate)) {
            log.info("【{}】同步终止, reason=时间区间为空, startDate={}, endDate={}", BIZ, startDate, endDate);
            throw new BizException(ResultCode.PARAM_MISSING, "startDate / endDate 不能为空");
        }

        if (KeruyunOrderConstants.DATE_TYPE_FINISH_BUSI_DATE.equals(dateType)) {
            LocalDate start = parseDay(startDate);
            LocalDate end = parseDay(endDate);
            if (start.isAfter(end)) {
                log.info("【{}】同步终止, reason=开始日期晚于结束日期, startDate={}, endDate={}",
                        BIZ, startDate, endDate);
                throw new BizException(ResultCode.PARAM_INVALID, "startDate 不能晚于 endDate");
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
            if (days > KeruyunOrderConstants.QUERY_MAX_DAYS) {
                log.info("【{}】同步终止, reason=查询跨度超限, days={}, maxDays={}",
                        BIZ, days, KeruyunOrderConstants.QUERY_MAX_DAYS);
                throw new BizException(ResultCode.PARAM_INVALID,
                        "查询时间范围不能超过 " + KeruyunOrderConstants.QUERY_MAX_DAYS + " 天");
            }
            return;
        }

        LocalDateTime start = parseSecond(startDate);
        LocalDateTime end = parseSecond(endDate);
        if (start.isAfter(end)) {
            log.info("【{}】同步终止, reason=开始时间晚于结束时间, startDate={}, endDate={}",
                    BIZ, startDate, endDate);
            throw new BizException(ResultCode.PARAM_INVALID, "startDate 不能晚于 endDate");
        }
        if (Duration.between(start, end).compareTo(Duration.ofDays(KeruyunOrderConstants.QUERY_MAX_DAYS)) > 0) {
            log.info("【{}】同步终止, reason=查询跨度超限, startDate={}, endDate={}, maxDays={}",
                    BIZ, startDate, endDate, KeruyunOrderConstants.QUERY_MAX_DAYS);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "查询时间范围不能超过 " + KeruyunOrderConstants.QUERY_MAX_DAYS + " 天");
        }
    }

    /**
     * 解析营业日期（yyyy-MM-dd）
     */
    private LocalDate parseDay(String value) {
        try {
            return LocalDate.parse(value.trim(), DAY_FORMATTER);
        } catch (DateTimeParseException e) {
            log.info("【{}】同步终止, reason=日期格式错误, value={}, pattern={}",
                    BIZ, value, KeruyunOrderConstants.DATE_PATTERN_DAY);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "dateType=FINISH_BUSI_DATE 时时间格式须为 " + KeruyunOrderConstants.DATE_PATTERN_DAY);
        }
    }

    /**
     * 解析秒级时间（yyyy-MM-dd HH:mm:ss）
     */
    private LocalDateTime parseSecond(String value) {
        try {
            return LocalDateTime.parse(value.trim(), SECOND_FORMATTER);
        } catch (DateTimeParseException e) {
            log.info("【{}】同步终止, reason=时间格式错误, value={}, pattern={}",
                    BIZ, value, KeruyunOrderConstants.DATE_PATTERN_SECOND);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "非营业日期查询时时间格式须为 " + KeruyunOrderConstants.DATE_PATTERN_SECOND);
        }
    }

    /**
     * 解析每页条数：null 取默认值，超上限截断
     */
    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return KeruyunOrderConstants.ORDER_PAGE_SIZE_DEFAULT;
        }
        return Math.min(pageSize, KeruyunOrderConstants.ORDER_PAGE_SIZE_MAX);
    }

    /**
     * 解析总页数，解析失败回退当前页（保证至少处理当前页后退出）
     */
    private int parseIntOrDefault(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("【{}】总页数解析失败按当前页处理, value={}", BIZ, value);
            return defaultValue;
        }
    }
}
