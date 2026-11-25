package com.cacch.integration.service.heart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.config.heart.HeartProperties;
import com.cacch.integration.common.constant.heart.HeartContractConstants;
import com.cacch.integration.common.constant.heart.HeartOrderConstants;
import com.cacch.integration.common.constant.heart.HeartOrderLogConstants;
import com.cacch.integration.common.constant.heart.HeartPoProcessStatusConstants;
import com.cacch.integration.common.dto.heart.HeartOrderCreateResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.heart.HeartOrderSubmitDO;
import com.cacch.integration.entity.heart.HeartPoDO;
import com.cacch.integration.entity.heart.HeartPoItemDO;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddRequest;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddResponse;
import com.cacch.integration.integration.crm.client.dto.CrmContractDetailItem;
import com.cacch.integration.mapper.heart.HeartOrderSubmitMapper;
import com.cacch.integration.mapper.heart.HeartPoItemMapper;
import com.cacch.integration.mapper.heart.HeartPoMapper;
import com.cacch.integration.service.crm.api.ICrmContractService;
import com.cacch.integration.service.heart.api.IHeartOrderCreateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Heart 客户 PO 建单服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartOrderCreateServiceImpl implements IHeartOrderCreateService {

    private static final String LOG_BIZ = HeartOrderLogConstants.LOG_BIZ;

    private static final int FAIL_REASON_MAX_LENGTH = 1000;

    private final HeartPoMapper heartPoMapper;

    private final HeartPoItemMapper heartPoItemMapper;

    private final HeartOrderSubmitMapper heartOrderSubmitMapper;

    private final ICrmContractService crmContractService;

    private final HeartProperties heartProperties;

    @Override
    public HeartOrderCreateResult createOrder(Long poId) {
        if (poId == null) {
            log.info("【{}】建单终止：poId 为空", LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "poId 不能为空");
        }
        HeartPoDO po = heartPoMapper.selectById(poId);
        if (po == null) {
            log.info("【{}】建单终止：PO 不存在, poId={}", LOG_BIZ, poId);
            throw new BizException(ResultCode.PARAM_INVALID, "PO 不存在, poId=" + poId);
        }
        HeartOrderSubmitDO successRecord = findSuccessRecord(poId);
        if (successRecord != null) {
            log.info("【{}】建单终止：已存在成功提交记录，幂等跳过, poId={}, poNo={}, crmContractNo={}",
                    LOG_BIZ, poId, po.getPoNo(), successRecord.getCrmContractNo());
            return HeartOrderCreateResult.skipped(successRecord.getCrmContractId(), successRecord.getCrmContractNo());
        }
        if (!HeartPoProcessStatusConstants.PORT_RESOLVED.equals(po.getProcessStatus())) {
            log.info("【{}】建单终止：状态不可建单, poId={}, poNo={}, processStatus={}",
                    LOG_BIZ, poId, po.getPoNo(), po.getProcessStatus());
            throw new BizException(ResultCode.PARAM_INVALID,
                    "PO 状态不可建单, processStatus=" + po.getProcessStatus());
        }

        String traceId = currentTraceId();
        long start = System.currentTimeMillis();
        CrmContractAddRequest request = null;
        try {
            List<HeartPoItemDO> items = listItems(poId);
            if (CollectionUtils.isEmpty(items)) {
                throw new BizException(ResultCode.PARAM_MISSING, "PO 明细为空, poNo=" + po.getPoNo());
            }
            request = buildRequest(po, items);
            savePayloadSnapshot(poId, request);
        } catch (BizException e) {
            log.error("【{}】建单前置校验失败, poId={}, poNo={}, error={}", LOG_BIZ, poId, po.getPoNo(), e.getMessage());
            return fail(po, request, e.getMessage(), System.currentTimeMillis() - start, traceId);
        } catch (Exception e) {
            log.error("【{}】建单报文组装异常, poId={}, poNo={}", LOG_BIZ, poId, po.getPoNo(), e);
            return fail(po, request, "建单报文组装异常: " + e.getMessage(),
                    System.currentTimeMillis() - start, traceId);
        }

        CrmContractAddResponse response;
        try {
            response = crmContractService.addContract(request);
        } catch (BizException e) {
            log.error("【{}】CRM 建单失败, poId={}, poNo={}, error={}", LOG_BIZ, poId, po.getPoNo(), e.getMessage());
            return fail(po, request, e.getMessage(), System.currentTimeMillis() - start, traceId);
        } catch (Exception e) {
            log.error("【{}】CRM 建单异常, poId={}, poNo={}", LOG_BIZ, poId, po.getPoNo(), e);
            return fail(po, request, "CRM 建单异常: " + e.getMessage(),
                    System.currentTimeMillis() - start, traceId);
        }

        long durationMs = System.currentTimeMillis() - start;
        JsonNode responseData = unwrapData(response.getResponseData());
        String crmContractId = textValue(responseData, "id", "contractId");
        String crmContractNo = textValue(responseData, "contractNo", "contract_no", "code", "number");
        if (!StringUtils.hasText(crmContractNo)) {
            log.info("【{}】CRM 建单成功但未解析到合同编号, poId={}, poNo={}, crmContractId={}",
                    LOG_BIZ, poId, po.getPoNo(), crmContractId);
        }

        try {
            recordSubmit(po, request, buildResponsePayload(response, responseData),
                    HeartOrderConstants.RECORD_STATUS_SUCCESS, crmContractId, crmContractNo,
                    response.getReturnCode(), response.getReturnMsg(), null, durationMs, traceId);
        } catch (Exception e) {
            log.error("【{}】建单成功但提交记录落库失败, poId={}, poNo={}, crmContractNo={}",
                    LOG_BIZ, poId, po.getPoNo(), crmContractNo, e);
        }
        try {
            markOrderCreated(po);
        } catch (Exception e) {
            log.error("【{}】建单成功但状态推进失败, poId={}, poNo={}, crmContractNo={}",
                    LOG_BIZ, poId, po.getPoNo(), crmContractNo, e);
        }
        log.info("【{}】建单成功, poId={}, poNo={}, crmContractId={}, crmContractNo={}, durationMs={}",
                LOG_BIZ, poId, po.getPoNo(), crmContractId, crmContractNo, durationMs);
        return HeartOrderCreateResult.created(crmContractId, crmContractNo);
    }

    /**
     * 查询已成功的提交记录
     *
     * @param poId PO 主键
     * @return 成功记录，不存在返回 null
     */
    private HeartOrderSubmitDO findSuccessRecord(Long poId) {
        return heartOrderSubmitMapper.selectOne(Wrappers.<HeartOrderSubmitDO>lambdaQuery()
                .eq(HeartOrderSubmitDO::getPoId, poId)
                .eq(HeartOrderSubmitDO::getSubmitStatus, HeartOrderConstants.RECORD_STATUS_SUCCESS)
                .orderByDesc(HeartOrderSubmitDO::getSubmittedAt)
                .last("LIMIT 1"));
    }

    /**
     * 查询 PO 明细行
     *
     * @param poId PO 主键
     * @return 明细行列表
     */
    private List<HeartPoItemDO> listItems(Long poId) {
        return heartPoItemMapper.selectList(Wrappers.<HeartPoItemDO>lambdaQuery()
                .eq(HeartPoItemDO::getPoId, poId)
                .orderByAsc(HeartPoItemDO::getLineNo));
    }

    /**
     * 组装 contractAdd 请求
     *
     * @param po    PO 主表
     * @param items 明细行
     * @return 建单请求
     */
    private CrmContractAddRequest buildRequest(HeartPoDO po, List<HeartPoItemDO> items) {
        if (!StringUtils.hasText(po.getCacPoNo())) {
            throw new BizException(ResultCode.PARAM_MISSING, "CAC PO 号为空, poNo=" + po.getPoNo());
        }
        if (!StringUtils.hasText(po.getPortOfDischargeId())) {
            throw new BizException(ResultCode.PARAM_MISSING, "目的港对象 ID 为空, poNo=" + po.getPoNo());
        }
        if (!StringUtils.hasText(po.getAttachmentUrl())) {
            throw new BizException(ResultCode.PARAM_MISSING, "PO 附件 URL 为空, poNo=" + po.getPoNo());
        }

        CrmContractAddRequest request = new CrmContractAddRequest();
        request.setCustomerCode(heartProperties.getCrm().getCustomerCode());
        request.setIsTrigger(heartProperties.getCrm().getIsTrigger());
        request.setTriggerEmployee(Long.valueOf(heartProperties.getCrm().getTriggerEmployee()));
        request.setOwnerId(heartProperties.getCrm().getOwnerId());
        request.setContractDate(LocalDate.now().toString());
        request.setCurrencyType(HeartContractConstants.CURRENCY_CNY);
        request.setExchangeRate(HeartContractConstants.EXCHANGE_RATE_ONE);
        request.setExtFields(buildExtFields(po));
        request.setDetailList(items.stream().map(this::buildDetail).toList());
        request.setExtSlaveList(buildExtSlaveList(po, items));
        return request;
    }

    /**
     * 组装合同级自定义字段
     *
     * @param po PO 主表
     * @return 自定义字段
     */
    private Map<String, Object> buildExtFields(HeartPoDO po) {
        Map<String, Object> extFields = new LinkedHashMap<>(HeartContractConstants.FIXED_EXT_FIELDS);
        extFields.put(HeartContractConstants.FIELD_ATTACHMENT, buildAttachment(po));
        extFields.put(HeartContractConstants.FIELD_CLIENT_PO, po.getPoNo());
        extFields.put(HeartContractConstants.FIELD_CUSTOMER_BALANCE,
                HeartContractConstants.CUSTOMER_BALANCE_PLACEHOLDER);
        extFields.put(HeartContractConstants.FIELD_DISCHARGE_PORT, po.getPortOfDischargeId());
        extFields.put(HeartContractConstants.FIELD_DESTINATION, po.getPortOfDischargeId());
        extFields.put(HeartContractConstants.FIELD_CUSTOMS_PORT,
                StringUtils.hasText(po.getPortOfLoadingId())
                        ? po.getPortOfLoadingId() : HeartContractConstants.CUSTOMS_PORT_FALLBACK_ID);
        String grossMargin = po.getGrossMarginRate() == null
                ? HeartContractConstants.GROSS_MARGIN_PLACEHOLDER : po.getGrossMarginRate().toPlainString();
        extFields.put(HeartContractConstants.FIELD_GROSS_MARGIN_A, grossMargin);
        extFields.put(HeartContractConstants.FIELD_GROSS_MARGIN_B, grossMargin);
        extFields.put(HeartContractConstants.FIELD_CAC_PO, po.getCacPoNo());
        return extFields;
    }

    /**
     * 组装合同附件
     *
     * @param po PO 主表
     * @return 附件数组
     */
    private List<Map<String, Object>> buildAttachment(HeartPoDO po) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("path", po.getAttachmentUrl());
        file.put("name", po.getPoNo() + HeartContractConstants.ATTACHMENT_FILE_SUFFIX);
        return List.of(file);
    }

    /**
     * 组装单条销售明细
     *
     * @param item 明细行
     * @return 销售明细
     */
    private CrmContractDetailItem buildDetail(HeartPoItemDO item) {
        if (!StringUtils.hasText(item.getPdCode())) {
            throw new BizException(ResultCode.PARAM_MISSING, "明细物料编码为空, lineNo=" + item.getLineNo());
        }
        LineMetrics metrics = calculateLine(item);
        Map<String, Object> extFields = new LinkedHashMap<>(HeartContractConstants.FIXED_DETAIL_EXT_FIELDS);
        extFields.put("field_ik73t__c", metrics.actualPrice());
        extFields.put("field_BVXk8__c", metrics.lineAmount());
        extFields.put("field_wdqjr__c", item.getCostPrice() == null
                ? BigDecimal.ZERO : multiplyThousand(item.getCostPrice()));

        CrmContractDetailItem detail = new CrmContractDetailItem();
        detail.setPdCode(item.getPdCode());
        detail.setUnitName(HeartContractConstants.UNIT_NAME_KILOLITER);
        detail.setPdCount(metrics.pdCount());
        detail.setActualPrice(metrics.actualPrice());
        detail.setCurrencyType(HeartContractConstants.CURRENCY_CNY);
        detail.setExchangeRate(HeartContractConstants.EXCHANGE_RATE_ONE);
        detail.setExtFields(extFields);
        return detail;
    }

    /**
     * 组装从对象数据（出运计划、费用、报关明细）
     *
     * @param po    PO 主表
     * @param items 明细行
     * @return 从对象数据
     */
    private Map<String, Object> buildExtSlaveList(HeartPoDO po, List<HeartPoItemDO> items) {
        List<Map<String, Object>> shipmentRows = new ArrayList<>(items.size());
        List<Map<String, Object>> customsRows = new ArrayList<>(items.size());
        BigDecimal packageTotal = BigDecimal.ZERO;
        BigDecimal insuranceTotal = BigDecimal.ZERO;
        for (HeartPoItemDO item : items) {
            LineMetrics metrics = calculateLine(item);
            shipmentRows.add(buildShipmentRow(po, item, metrics));
            customsRows.add(buildCustomsRow(item, metrics));
            packageTotal = packageTotal.add(zeroIfNull(item.getPackageFee()));
            insuranceTotal = insuranceTotal.add(zeroIfNull(item.getCreditInsuranceFee()));
        }
        Map<String, Object> extSlaveList = new LinkedHashMap<>();
        extSlaveList.put(HeartContractConstants.OBJECT_SHIPMENT_PLAN, shipmentRows);
        extSlaveList.put(HeartContractConstants.OBJECT_FEE, buildFeeRows(packageTotal, insuranceTotal));
        extSlaveList.put(HeartContractConstants.OBJECT_CUSTOMS, customsRows);
        return extSlaveList;
    }

    /**
     * 组装出运计划行
     *
     * @param po      PO 主表
     * @param item    明细行
     * @param metrics 行计算结果
     * @return 出运计划行
     */
    private Map<String, Object> buildShipmentRow(HeartPoDO po, HeartPoItemDO item, LineMetrics metrics) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("field_qnF4Z__c", po.getEtd() == null ? LocalDate.now().toString() : po.getEtd().toString());
        row.put("field_VgRQI__c", StringUtils.hasText(item.getProductId()) ? item.getProductId() : item.getPdCode());
        row.put("field_KCPjb__c", metrics.pdCount());
        row.put("field_2AZ9H__c", metrics.actualPrice());
        row.put("field_PDoci__c", metrics.lineAmount());
        row.put("field_QeiRn__c", metrics.lineAmount());
        row.put("field_2nLx8__c", HeartContractConstants.SHIPMENT_RATE);
        row.put("currency_type", HeartContractConstants.CURRENCY_CNY);
        row.put("exchange_rate", HeartContractConstants.EXCHANGE_RATE_ONE);
        row.put("field_97r9V__c", metrics.lineAmount()
                .multiply(HeartContractConstants.SHIPMENT_RATE).setScale(4, RoundingMode.HALF_UP));
        return row;
    }

    /**
     * 组装报关明细行
     *
     * @param item    明细行
     * @param metrics 行计算结果
     * @return 报关明细行
     */
    private Map<String, Object> buildCustomsRow(HeartPoItemDO item, LineMetrics metrics) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("field_kpfT7__c", StringUtils.hasText(item.getProductId()) ? item.getProductId() : item.getPdCode());
        row.put("field_o5i8Y__c", metrics.actualPrice());
        row.put("currency_type", HeartContractConstants.CURRENCY_CNY);
        row.put("exchange_rate", HeartContractConstants.EXCHANGE_RATE_ONE);
        return row;
    }

    /**
     * 组装费用行（包干费、信保费、订舱费）
     *
     * @param packageTotal   包干费合计
     * @param insuranceTotal 信保费合计
     * @return 费用行
     */
    private List<Map<String, Object>> buildFeeRows(BigDecimal packageTotal, BigDecimal insuranceTotal) {
        List<Map<String, Object>> rows = new ArrayList<>(3);
        rows.add(buildFeeRow(HeartContractConstants.FEE_TYPE_PACKAGE,
                packageTotal.setScale(4, RoundingMode.HALF_UP), HeartContractConstants.SUPPLIER_PACKAGE));
        rows.add(buildFeeRow(HeartContractConstants.FEE_TYPE_PACKAGE,
                insuranceTotal.setScale(4, RoundingMode.HALF_UP), null));
        rows.add(buildFeeRow(HeartContractConstants.FEE_TYPE_BOOKING, BigDecimal.ZERO,
                HeartContractConstants.SUPPLIER_BOOKING));
        return rows;
    }

    /**
     * 组装单条费用行
     *
     * @param feeTypeId  费用类型 ID
     * @param amount     预算金额
     * @param supplierId 供应商 ID，可为空
     * @return 费用行
     */
    private Map<String, Object> buildFeeRow(String feeTypeId, BigDecimal amount, String supplierId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("field_qnF4Z__c", feeTypeId);
        row.put("field_M0UBt__c", amount);
        if (StringUtils.hasText(supplierId)) {
            row.put("field_0fU4o__c", supplierId);
        }
        row.put("currency_type", HeartContractConstants.CURRENCY_CNY);
        row.put("exchange_rate", HeartContractConstants.EXCHANGE_RATE_ONE);
        return row;
    }

    /**
     * 计算单行数量、单价与金额
     *
     * @param item 明细行
     * @return 行计算结果
     */
    private LineMetrics calculateLine(HeartPoItemDO item) {
        if (item.getLiters() == null || item.getUnitPrice() == null) {
            throw new BizException(ResultCode.PARAM_MISSING,
                    "明细数量或单价为空, lineNo=" + item.getLineNo());
        }
        BigDecimal pdCount = item.getLiters()
                .divide(HeartContractConstants.LITER_PER_KILOLITER, 3, RoundingMode.HALF_UP);
        BigDecimal actualPrice = multiplyThousand(item.getUnitPrice());
        BigDecimal lineAmount = pdCount.multiply(actualPrice).setScale(4, RoundingMode.HALF_UP);
        return new LineMetrics(pdCount, actualPrice, lineAmount);
    }

    /**
     * 数值乘以 1000（L→千升口径的单价折算）
     *
     * @param value 原值
     * @return 折算后金额
     */
    private BigDecimal multiplyThousand(BigDecimal value) {
        return value.multiply(HeartContractConstants.LITER_PER_KILOLITER).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 空值转 0
     *
     * @param value 原值
     * @return 非空金额
     */
    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 保存建单请求快照
     *
     * @param poId    PO 主键
     * @param request 建单请求
     */
    private void savePayloadSnapshot(Long poId, CrmContractAddRequest request) {
        HeartPoDO update = new HeartPoDO();
        update.setId(poId);
        update.setCrmPayload(request);
        heartPoMapper.updateById(update);
    }

    /**
     * 记录提交结果
     *
     * @param po              PO 主表
     * @param request         请求报文
     * @param responsePayload 响应报文
     * @param submitStatus    提交状态
     * @param crmContractId   CRM 订单数据 ID
     * @param crmContractNo   CRM 订单编号
     * @param returnCode      返回码
     * @param returnMsg       返回信息
     * @param errorMessage    错误信息
     * @param durationMs      耗时
     * @param traceId         链路 ID
     */
    private void recordSubmit(HeartPoDO po, Object request, Object responsePayload, String submitStatus,
                              String crmContractId, String crmContractNo, String returnCode,
                              String returnMsg, String errorMessage, long durationMs, String traceId) {
        HeartOrderSubmitDO record = new HeartOrderSubmitDO();
        record.setPoId(po.getId());
        record.setAttemptNo(nextAttemptNo(po.getId()));
        record.setSubmitStatus(submitStatus);
        record.setCrmContractId(crmContractId);
        record.setCrmContractNo(crmContractNo);
        record.setReturnCode(returnCode);
        record.setReturnMsg(returnMsg);
        record.setErrorMessage(truncate(errorMessage));
        record.setRequestPayload(request);
        record.setResponsePayload(responsePayload);
        record.setDurationMs((int) durationMs);
        record.setTraceId(traceId);
        record.setSubmittedAt(LocalDateTime.now());
        heartOrderSubmitMapper.insert(record);
    }

    /**
     * 计算下一次提交序号
     *
     * @param poId PO 主键
     * @return 提交序号
     */
    private int nextAttemptNo(Long poId) {
        Long count = heartOrderSubmitMapper.selectCount(Wrappers.<HeartOrderSubmitDO>lambdaQuery()
                .eq(HeartOrderSubmitDO::getPoId, poId));
        return count == null ? 1 : count.intValue() + 1;
    }

    /**
     * 建单失败处理：落失败记录并回写失败原因
     *
     * @param po         PO 主表
     * @param request    请求报文，可能为空
     * @param message    失败原因
     * @param durationMs 耗时
     * @param traceId    链路 ID
     * @return 失败结果
     */
    private HeartOrderCreateResult fail(HeartPoDO po, Object request, String message,
                                        long durationMs, String traceId) {
        try {
            recordSubmit(po, request, null, HeartOrderConstants.RECORD_STATUS_FAILED,
                    null, null, null, null, message, durationMs, traceId);
        } catch (Exception e) {
            log.error("【{}】提交失败记录落库异常, poId={}, poNo={}", LOG_BIZ, po.getId(), po.getPoNo(), e);
        }
        try {
            markFailed(po, message);
        } catch (Exception e) {
            log.error("【{}】失败原因回写异常, poId={}, poNo={}", LOG_BIZ, po.getId(), po.getPoNo(), e);
        }
        return HeartOrderCreateResult.failed(message);
    }

    /**
     * 推进 PO 状态为已建单
     *
     * @param po PO 主表
     */
    private void markOrderCreated(HeartPoDO po) {
        int rows = heartPoMapper.update(null, Wrappers.<HeartPoDO>lambdaUpdate()
                .eq(HeartPoDO::getId, po.getId())
                .eq(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.PORT_RESOLVED)
                .set(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.ORDER_CREATED)
                .set(HeartPoDO::getSubmitCount, (po.getSubmitCount() == null ? 0 : po.getSubmitCount()) + 1)
                .set(HeartPoDO::getLastSubmitAt, LocalDateTime.now())
                .set(HeartPoDO::getFailReason, null));
        if (rows == 0) {
            log.info("【{}】状态推进未生效（可能已被并发处理）, poId={}, poNo={}", LOG_BIZ, po.getId(), po.getPoNo());
        }
    }

    /**
     * 回写失败原因与重试次数
     *
     * @param po      PO 主表
     * @param message 失败原因
     */
    private void markFailed(HeartPoDO po, String message) {
        HeartPoDO update = new HeartPoDO();
        update.setId(po.getId());
        update.setFailReason(truncate(message));
        update.setRetryCount((po.getRetryCount() == null ? 0 : po.getRetryCount()) + 1);
        update.setSubmitCount((po.getSubmitCount() == null ? 0 : po.getSubmitCount()) + 1);
        update.setLastSubmitAt(LocalDateTime.now());
        heartPoMapper.updateById(update);
    }

    /**
     * 组装响应快照
     *
     * @param response 响应对象
     * @param data     响应数据
     * @return 响应快照
     */
    private Map<String, Object> buildResponsePayload(CrmContractAddResponse response, JsonNode data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("returnCode", response.getReturnCode());
        payload.put("returnMsg", response.getReturnMsg());
        payload.put("msgId", response.getMsgId());
        payload.put("responseData", data);
        return payload;
    }

    /**
     * 数组响应解包为首个元素
     *
     * @param data 响应数据
     * @return 业务对象节点
     */
    private JsonNode unwrapData(JsonNode data) {
        if (data != null && data.isArray() && !data.isEmpty()) {
            return data.get(0);
        }
        return data;
    }

    /**
     * 从节点中按候选字段名读取文本值
     *
     * @param node  节点
     * @param names 候选字段名
     * @return 文本值，未命中返回 null
     */
    private String textValue(JsonNode node, String... names) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                return value.asString();
            }
        }
        return null;
    }

    /**
     * 获取链路 ID
     *
     * @return traceId
     */
    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }

    /**
     * 截断超长文本
     *
     * @param text 原文本
     * @return 截断后文本
     */
    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > FAIL_REASON_MAX_LENGTH ? text.substring(0, FAIL_REASON_MAX_LENGTH) : text;
    }

    /**
     * 单行计算结果
     *
     * @param pdCount     千升数量
     * @param actualPrice 千升单价
     * @param lineAmount  行金额
     */
    private record LineMetrics(BigDecimal pdCount, BigDecimal actualPrice, BigDecimal lineAmount) {
    }
}
