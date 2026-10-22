package com.cacch.integration.manager.keruyun.api.impl;

import com.cacch.integration.common.constant.keruyun.KeruyunOrderConstants;
import com.cacch.integration.common.constant.keruyun.KeruyunOrderSyncStatus;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncItem;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderSyncTaskDO;
import com.cacch.integration.integration.keruyun.client.KeruyunOrderClient;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderItem;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderQueryRequest;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderQueryResponse;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunPageBean;
import com.cacch.integration.manager.keruyun.api.IKeruyunManager;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderManager;
import com.cacch.integration.manager.keruyun.convert.KeruyunOrderConverter;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderService;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderSyncTaskService;
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
    private final IKeruyunOrderSyncTaskService taskService;

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

        KeruyunOrderSyncResult result = new KeruyunOrderSyncResult(dateType, startDate, endDate);
        int pageNum = 1;
        int pageCount = 0;

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
                KeruyunOrderSyncItem syncItem = new KeruyunOrderSyncItem();
                syncItem.setOrderId(item.getOrderId());
                syncItem.setBusiOrderNo(item.getBusiOrderNo());
                syncItem.setShopId(item.getShopId());
                syncItem.setPageNum(pageNum);

                if (!StringUtils.hasText(item.getOrderId())) {
                    log.info("【{}】跳过订单, reason=orderId为空, pageNum={}, busiOrderNo={}",
                            BIZ, pageNum, item.getBusiOrderNo());
                    result.recordSkip(syncItem, "orderId为空");
                    continue;
                }
                try {
                    KeruyunOrderDO orderDO = keruyunOrderConverter.toDO(item);
                    orderDO.setSyncBatch(syncBatch);
                    orderDO.setSyncedAt(LocalDateTime.now());
                    boolean inserted = keruyunOrderService.saveOrUpdate(orderDO);
                    syncItem.setOperation(inserted
                            ? KeruyunOrderSyncItem.Operation.INSERTED
                            : KeruyunOrderSyncItem.Operation.UPDATED);
                    result.recordSuccess(syncItem);
                } catch (Exception e) {
                    log.info("【{}】单条落库终止, orderId={}, pageNum={}, reason={}",
                            BIZ, item.getOrderId(), pageNum, e.getMessage());
                    log.error("【{}】单条落库失败, orderId={}, pageNum={}",
                            BIZ, item.getOrderId(), pageNum, e);
                    result.recordFailure(syncItem, e.getMessage());
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

        result.setPageCount(pageCount);
        log.info("【{}】同步完成, dateType={}, startDate={}, endDate={}, pageCount={}, totalCount={}, "
                        + "insertedCount={}, updatedCount={}, skipCount={}, failureCount={}, detailsTruncated={}",
                BIZ, dateType, startDate, endDate, pageCount, result.getTotalCount(),
                result.getInsertedCount(), result.getUpdatedCount(), result.getSkipCount(),
                result.getFailureCount(), result.isDetailsTruncated());
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

    @Override
    public void syncByTaskId(Long taskId) {
        KeruyunOrderSyncTaskDO task = taskService.getById(taskId);
        if (task == null) {
            log.info("【{}】任务驱动同步终止, reason=任务不存在, taskId={}", BIZ, taskId);
            throw new BizException(ResultCode.PARAM_INVALID, "同步任务不存在: " + taskId);
        }

        // CAS 状态流转：PENDING → IN_PROGRESS；或 IN_PROGRESS 且 lastPage > 0（断点续传）
        boolean canRun = taskService.transitionStatus(taskId,
                KeruyunOrderSyncStatus.PENDING.name(),
                KeruyunOrderSyncStatus.IN_PROGRESS.name());
        if (!canRun) {
            // 尝试断点续传（IN_PROGRESS + lastPage > 0 是允许的内部逻辑）
            KeruyunOrderSyncTaskDO refetch = taskService.getById(taskId);
            boolean isResumable = KeruyunOrderSyncStatus.IN_PROGRESS.name().equals(refetch.getStatus())
                    && refetch.getLastPage() != null && refetch.getLastPage() > 0;
            if (!isResumable) {
                log.info("【{}】任务驱动同步终止, reason=任务正在执行或状态非法, taskId={}, status={}",
                        BIZ, taskId, refetch.getStatus());
                throw new BizException(ResultCode.PARAM_INVALID,
                        "任务状态不允许执行: " + refetch.getStatus());
            }
        }

        // 构建请求体
        boolean isDay = KeruyunOrderConstants.DATE_TYPE_FINISH_BUSI_DATE.equals(task.getDateType());
        String startStr = isDay
                ? task.getSyncDate().format(DAY_FORMATTER)
                : task.getSyncWindowStart().format(SECOND_FORMATTER);
        String endStr = isDay
                ? task.getSyncDate().format(DAY_FORMATTER)
                : task.getSyncWindowEnd().format(SECOND_FORMATTER);

        String token = keruyunManager.getAccessToken();
        KeruyunOrderQueryRequest request = new KeruyunOrderQueryRequest();
        request.setDateType(task.getDateType());
        request.setStartDate(startStr);
        request.setEndDate(endStr);

        // 断点续传：从 last_page + 1 开始
        int startPage = (task.getLastPage() == null || task.getLastPage() <= 0) ? 1 : task.getLastPage() + 1;
        int pageSize = KeruyunOrderConstants.ORDER_PAGE_SIZE_DEFAULT;
        String syncBatch = task.getDateType() + "-" + task.getSyncDate() + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        KeruyunOrderSyncResult result = new KeruyunOrderSyncResult(task.getDateType(), startStr, endStr);
        int pageNum = startPage;
        int pageCount = 0;
        int cumulativeSuccess = task.getSuccessCount() == null ? 0 : task.getSuccessCount();
        int cumulativeFailure = task.getFailureCount() == null ? 0 : task.getFailureCount();

        try {
            while (pageNum <= KeruyunOrderConstants.ORDER_MAX_PAGE_GUARD) {
                request.setPageBean(new KeruyunPageBean(String.valueOf(pageNum), String.valueOf(pageSize)));
                KeruyunOrderQueryResponse response = keruyunOrderClient.queryOrderList(request, token);
                KeruyunOrderQueryResponse.DataInfo data =
                        response.getResult() == null ? null : response.getResult().getData();
                if (data == null || data.getList() == null || data.getList().isEmpty()) {
                    log.info("【{}】任务驱动查询无更多数据, taskId={}, dateType={}, pageNum={}",
                            BIZ, taskId, task.getDateType(), pageNum);
                    break;
                }

                pageCount++;
                for (KeruyunOrderItem item : data.getList()) {
                    KeruyunOrderSyncItem syncItem = new KeruyunOrderSyncItem();
                    syncItem.setOrderId(item.getOrderId());
                    syncItem.setBusiOrderNo(item.getBusiOrderNo());
                    syncItem.setShopId(item.getShopId());
                    syncItem.setPageNum(pageNum);

                    if (!StringUtils.hasText(item.getOrderId())) {
                        log.info("【{}】跳过订单, reason=orderId为空, taskId={}, pageNum={}",
                                BIZ, taskId, pageNum);
                        result.recordSkip(syncItem, "orderId为空");
                        continue;
                    }
                    try {
                        KeruyunOrderDO orderDO = keruyunOrderConverter.toDO(item);
                        orderDO.setSyncBatch(syncBatch);
                        orderDO.setSyncedAt(LocalDateTime.now());
                        boolean inserted = keruyunOrderService.saveOrUpdate(orderDO);
                        syncItem.setOperation(inserted
                                ? KeruyunOrderSyncItem.Operation.INSERTED
                                : KeruyunOrderSyncItem.Operation.UPDATED);
                        result.recordSuccess(syncItem);
                    } catch (Exception e) {
                        log.info("【{}】单条落库终止, taskId={}, orderId={}, pageNum={}, reason={}",
                                BIZ, taskId, item.getOrderId(), pageNum, e.getMessage());
                        log.error("【{}】单条落库失败, taskId={}, orderId={}, pageNum={}",
                                BIZ, taskId, item.getOrderId(), pageNum, e);
                        result.recordFailure(syncItem, e.getMessage());
                    }
                }

                // 每页更新进度（断点续传用）
                cumulativeSuccess += result.getInsertedCount() + result.getUpdatedCount();
                cumulativeFailure += result.getFailureCount();
                Integer totalPage = parseIntOrDefault(data.getTotalPage(), pageNum);
                int totalCount = 0;
                if (StringUtils.hasText(data.getTotalCount())) {
                    try {
                        totalCount = Integer.parseInt(data.getTotalCount().trim());
                    } catch (NumberFormatException ignore) {
                    }
                }
                taskService.updateProgress(taskId, pageNum, totalPage,
                        cumulativeSuccess, cumulativeFailure);

                if (pageNum >= totalPage) {
                    taskService.updateProgress(taskId, pageNum, totalPage,
                            cumulativeSuccess, cumulativeFailure);
                    break;
                }
                pageNum++;
            }

            if (pageNum > KeruyunOrderConstants.ORDER_MAX_PAGE_GUARD) {
                log.warn("【{}】任务驱动翻页达到保护上限提前终止, taskId={}, maxPage={}",
                        BIZ, taskId, KeruyunOrderConstants.ORDER_MAX_PAGE_GUARD);
            }

            result.setPageCount(pageCount);

            // 标记终态
            String finalStatus = result.getFailureCount() == 0
                    ? KeruyunOrderSyncStatus.SUCCESS.name()
                    : KeruyunOrderSyncStatus.PARTIAL.name();
            taskService.markComplete(taskId, finalStatus,
                    result.getFailureCount() == 0 ? null : "存在 " + result.getFailureCount() + " 条落库失败");

            log.info("【{}】任务驱动同步完成, taskId={}, dateType={}, syncDate={}, pageCount={}, "
                            + "totalCount={}, insertedCount={}, updatedCount={}, skipCount={}, failureCount={}, status={}",
                    BIZ, taskId, task.getDateType(), task.getSyncDate(), pageCount, result.getTotalCount(),
                    result.getInsertedCount(), result.getUpdatedCount(), result.getSkipCount(),
                    result.getFailureCount(), finalStatus);

        } catch (Exception e) {
            log.error("【{}】任务驱动同步异常, taskId={}, dateType={}, syncDate={}",
                    BIZ, taskId, task.getDateType(), task.getSyncDate(), e);
            taskService.markComplete(taskId, KeruyunOrderSyncStatus.FAILED.name(), e.getMessage());
            throw e;
        }

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
