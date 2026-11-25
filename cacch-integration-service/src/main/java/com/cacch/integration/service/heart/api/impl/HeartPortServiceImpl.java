package com.cacch.integration.service.heart.api.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.constant.heart.HeartPoProcessStatusConstants;
import com.cacch.integration.common.constant.heart.HeartPortConstants;
import com.cacch.integration.common.constant.heart.HeartPortLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.heart.HeartPoDO;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryResponse;
import com.cacch.integration.integration.crm.client.dto.CrmQueryFilter;
import com.cacch.integration.integration.crm.client.dto.CrmQueryGroup;
import com.cacch.integration.mapper.heart.HeartPoMapper;
import com.cacch.integration.service.crm.api.ICrmCustomObjectService;
import com.cacch.integration.service.heart.api.IHeartPortService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Heart 客户 PO 港口解析服务实现
 *
 * <p>港口关键字当前取自 PO 主表的 {@code port_of_loading / port_of_discharge}
 * （后续由 AI 解析结果写入），查询勤策 {@code object_yEQGF__c} 得到对象 ID 并回写。
 * 幂等策略：已存在对象 ID 的港口跳过查询；回写以「该列仍为空」为条件，
 * 并发场景下重查并复用已写入值。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartPortServiceImpl implements IHeartPortService {

    private final HeartPoMapper heartPoMapper;

    private final ICrmCustomObjectService crmCustomObjectService;

    @Override
    public void resolveAndSavePorts(Long poId) {
        if (poId == null) {
            log.info("【{}】港口解析终止, reason=poId为空", HeartPortLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "poId 不能为空");
        }
        HeartPoDO po = heartPoMapper.selectById(poId);
        if (po == null) {
            log.info("【{}】港口解析终止, reason=PO不存在, poId={}", HeartPortLogConstants.LOG_BIZ, poId);
            throw new BizException(ResultCode.PARAM_INVALID, "PO 不存在, poId=" + poId);
        }
        boolean loadingMissing = !StringUtils.hasText(po.getPortOfLoadingId());
        boolean dischargeMissing = !StringUtils.hasText(po.getPortOfDischargeId());
        if (!loadingMissing && !dischargeMissing) {
            log.info("【{}】港口解析复用已有对象ID, poId={}, loadingId={}, dischargeId={}",
                    HeartPortLogConstants.LOG_BIZ, poId, po.getPortOfLoadingId(), po.getPortOfDischargeId());
            advanceToPortResolved(poId);
            return;
        }

        String loadingId = loadingMissing
                ? resolvePortObjectId(po.getPortOfLoading(), HeartPortConstants.PORT_TYPE_LOADING) : null;
        String dischargeId = dischargeMissing
                ? resolvePortObjectId(po.getPortOfDischarge(), HeartPortConstants.PORT_TYPE_DISCHARGE) : null;

        if (loadingId != null) {
            updatePortId(poId, true, loadingId);
        }
        if (dischargeId != null) {
            updatePortId(poId, false, dischargeId);
        }

        HeartPoDO latest = heartPoMapper.selectById(poId);
        if (latest == null
                || !StringUtils.hasText(latest.getPortOfLoadingId())
                || !StringUtils.hasText(latest.getPortOfDischargeId())) {
            log.info("【{}】港口解析回写失败, poId={}, loadingId={}, dischargeId={}",
                    HeartPortLogConstants.LOG_BIZ, poId, loadingId, dischargeId);
            throw new BizException(ResultCode.SYSTEM_ERROR, "港口对象ID回写失败, poId=" + poId);
        }
        advanceToPortResolved(poId);
        log.info("【{}】港口解析完成, poId={}, loadingId={}, dischargeId={}",
                HeartPortLogConstants.LOG_BIZ, poId, latest.getPortOfLoadingId(), latest.getPortOfDischargeId());
    }

    /**
     * 两港对象 ID 均已回写后，将 PO 推进为 PORT_RESOLVED
     *
     * @param poId PO 主键
     */
    private void advanceToPortResolved(Long poId) {
        int rows = heartPoMapper.update(null, Wrappers.<HeartPoDO>lambdaUpdate()
                .eq(HeartPoDO::getId, poId)
                .eq(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.SERIAL_NUMBERED)
                .set(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.PORT_RESOLVED)
                .set(HeartPoDO::getUpdatedAt, LocalDateTime.now()));
        log.info("【{}】港口解析推进状态, poId={}, status={}, rows={}", HeartPortLogConstants.LOG_BIZ,
                poId, HeartPoProcessStatusConstants.PORT_RESOLVED, rows);
    }

    @Override
    public String resolvePortObjectId(String portName, String portType) {
        if (!StringUtils.hasText(portName)) {
            log.info("【{}】{}解析终止, reason=港口名称为空", HeartPortLogConstants.LOG_BIZ, portType);
            throw new BizException(ResultCode.PARAM_MISSING, portType + "名称为空, 请先维护 PO 上的港口名称");
        }
        String keyword = portName.trim();
        CrmCustomObjectQueryRequest request = new CrmCustomObjectQueryRequest();
        request.setObjectKey(HeartPortConstants.OBJECT_KEY_PORT);
        request.setPage(HeartPortConstants.DEFAULT_PAGE);
        request.setRows(HeartPortConstants.DEFAULT_ROWS);
        request.setQueryGroup(List.of(CrmQueryGroup.builder()
                .filters(List.of(CrmQueryFilter.builder()
                        .fieldKey(HeartPortConstants.FIELD_NAME)
                        .operator(HeartPortConstants.OPERATOR_LIKE)
                        .fieldValues(List.of(keyword))
                        .build()))
                .build()));

        log.info("【{}】查询{}对象ID, keyword={}", HeartPortLogConstants.LOG_BIZ, portType, keyword);
        try {
            CrmCustomObjectQueryResponse response = crmCustomObjectService.queryObjectData(request);
            Map<String, Object> matched = matchRecord(response.getResponseData(), keyword, portType);
            String id = toText(matched.get(HeartPortConstants.RESPONSE_FIELD_ID));
            if (!StringUtils.hasText(id)) {
                log.info("【{}】{}解析终止, reason=匹配记录缺少id, keyword={}",
                        HeartPortLogConstants.LOG_BIZ, portType, keyword);
                throw new BizException(ResultCode.INTEGRATION_ERROR, portType + "对象ID缺失, keyword=" + keyword);
            }
            log.info("【{}】{}对象ID解析成功, keyword={}, id={}, name={}", HeartPortLogConstants.LOG_BIZ,
                    portType, keyword, id, toText(matched.get(HeartPortConstants.RESPONSE_FIELD_NAME)));
            return id;
        } catch (BizException e) {
            log.error("【{}】{}解析终止, keyword={}, reason={}",
                    HeartPortLogConstants.LOG_BIZ, portType, keyword, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, portType + "查询调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按「该列仍为空」为条件回写港口对象 ID，避免覆盖并发写入值
     *
     * @param poId     PO 主键
     * @param loading  true 表示启运港，false 表示目的港
     * @param objectId 港口对象 ID
     */
    private void updatePortId(Long poId, boolean loading, String objectId) {
        LambdaUpdateWrapper<HeartPoDO> wrapper = Wrappers.<HeartPoDO>lambdaUpdate()
                .eq(HeartPoDO::getId, poId)
                .set(HeartPoDO::getUpdatedAt, LocalDateTime.now());
        if (loading) {
            wrapper.and(w -> w.isNull(HeartPoDO::getPortOfLoadingId).or().eq(HeartPoDO::getPortOfLoadingId, ""))
                    .set(HeartPoDO::getPortOfLoadingId, objectId);
        } else {
            wrapper.and(w -> w.isNull(HeartPoDO::getPortOfDischargeId).or().eq(HeartPoDO::getPortOfDischargeId, ""))
                    .set(HeartPoDO::getPortOfDischargeId, objectId);
        }
        int rows = heartPoMapper.update(null, wrapper);
        log.info("【{}】回写{}对象ID, poId={}, objectId={}, rows={}",
                HeartPortLogConstants.LOG_BIZ, loading
                        ? HeartPortConstants.PORT_TYPE_LOADING : HeartPortConstants.PORT_TYPE_DISCHARGE,
                poId, objectId, rows);
    }

    /**
     * 从查询结果中确定唯一匹配记录
     *
     * <p>优先取名称精确匹配（忽略大小写）；无精确匹配且结果仅一条时取该条；
     * 其余情况视为无法唯一确定，转人工处理。</p>
     *
     * @param records  查询结果
     * @param keyword  港口名称关键字
     * @param portType 港口类型标识
     * @return 唯一匹配记录
     */
    private Map<String, Object> matchRecord(List<Map<String, Object>> records, String keyword, String portType) {
        if (records == null || records.isEmpty()) {
            log.info("【{}】{}解析终止, reason=未查询到匹配港口, keyword={}",
                    HeartPortLogConstants.LOG_BIZ, portType, keyword);
            throw new BizException(ResultCode.INTEGRATION_ERROR, portType + "未查询到匹配港口, keyword=" + keyword);
        }
        List<Map<String, Object>> exact = records.stream()
                .filter(record -> keyword.equalsIgnoreCase(toText(record.get(HeartPortConstants.RESPONSE_FIELD_NAME))))
                .toList();
        if (exact.size() == 1) {
            return exact.getFirst();
        }
        if (records.size() == 1) {
            return records.getFirst();
        }
        log.info("【{}】{}解析终止, reason=匹配多条且无唯一精确匹配, keyword={}, count={}",
                HeartPortLogConstants.LOG_BIZ, portType, keyword, records.size());
        throw new BizException(ResultCode.INTEGRATION_ERROR,
                portType + "匹配到 " + records.size() + " 条且无唯一精确匹配, 请人工确认, keyword=" + keyword);
    }

    /**
     * 提取字段文本值，兼容标量、{@code {value/label/name}} 对象与数组三种形态
     *
     * @param value 原始字段值
     * @return 文本值，无法提取时返回 null
     */
    private String toText(Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case Map<?, ?> map -> {
                for (String key : List.of("value", "label", "name")) {
                    if (map.get(key) != null) {
                        return toText(map.get(key));
                    }
                }
                return null;
            }
            case List<?> list -> {
                return list.isEmpty() ? null : toText(list.getFirst());
            }
            default -> {
            }
        }
        return String.valueOf(value);
    }
}
