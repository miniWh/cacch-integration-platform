package com.cacch.integration.service.heart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.constant.heart.HeartBomConstants;
import com.cacch.integration.common.constant.heart.HeartBomLogConstants;
import com.cacch.integration.common.constant.heart.HeartPortConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.heart.HeartPoItemDO;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryResponse;
import com.cacch.integration.integration.crm.client.dto.CrmQueryFilter;
import com.cacch.integration.integration.crm.client.dto.CrmQueryGroup;
import com.cacch.integration.mapper.heart.HeartPoItemMapper;
import com.cacch.integration.service.crm.api.ICrmCustomObjectService;
import com.cacch.integration.service.heart.api.IHeartBomVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Heart 客户 PO 明细 BOM 版本解析服务实现
 *
 * <p>以商品编码前 9 位 + 名称固定词「哈特」查询 BOM 对象，响应中取名称包含 GA/GX
 * 的第一条记录 ID 回写明细；查询为空或无 GA/GX 记录时返回跳过原因，不改状态，
 * 便于人工修正数据后自动重试。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartBomVersionServiceImpl implements IHeartBomVersionService {

    private final HeartPoItemMapper heartPoItemMapper;

    private final ICrmCustomObjectService crmCustomObjectService;

    @Override
    public String resolveAndSaveBomVersions(Long poId) {
        if (poId == null) {
            log.info("【{}】BOM版本解析终止, reason=poId为空", HeartBomLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "poId 不能为空");
        }
        List<HeartPoItemDO> items = heartPoItemMapper.selectList(Wrappers.<HeartPoItemDO>lambdaQuery()
                .eq(HeartPoItemDO::getPoId, poId)
                .orderByAsc(HeartPoItemDO::getLineNo));
        if (items.isEmpty()) {
            log.info("【{}】BOM版本解析终止, reason=PO明细为空, poId={}", HeartBomLogConstants.LOG_BIZ, poId);
            throw new BizException(ResultCode.PARAM_MISSING, "PO 明细为空, poId=" + poId);
        }

        // 按编码前缀归集仍缺失版本的明细，相同前缀只查询一次
        Map<String, List<HeartPoItemDO>> pendingByPrefix = new LinkedHashMap<>();
        for (HeartPoItemDO item : items) {
            if (StringUtils.hasText(item.getBomVersionId())) {
                continue;
            }
            String pdCode = StringUtils.hasText(item.getPdCode()) ? item.getPdCode().trim() : null;
            if (pdCode == null || pdCode.length() < HeartBomConstants.PRODUCT_CODE_PREFIX_LENGTH) {
                String reason = "明细第 " + item.getLineNo() + " 行商品编码不足 "
                        + HeartBomConstants.PRODUCT_CODE_PREFIX_LENGTH + " 位, 已跳过建单";
                log.info("【{}】BOM版本解析跳过, poId={}, lineNo={}, pdCode={}",
                        HeartBomLogConstants.LOG_BIZ, poId, item.getLineNo(), item.getPdCode());
                return reason;
            }
            String prefix = pdCode.substring(0, HeartBomConstants.PRODUCT_CODE_PREFIX_LENGTH);
            pendingByPrefix.computeIfAbsent(prefix, key -> new ArrayList<>()).add(item);
        }
        if (pendingByPrefix.isEmpty()) {
            log.info("【{}】BOM版本复用已有值, poId={}, itemCount={}", HeartBomLogConstants.LOG_BIZ, poId, items.size());
            return null;
        }

        for (Map.Entry<String, List<HeartPoItemDO>> entry : pendingByPrefix.entrySet()) {
            String prefix = entry.getKey();
            String bomVersionId = queryBomVersionId(prefix);
            if (bomVersionId == null) {
                return "商品编码前缀 " + prefix + " 未匹配到含 GA/GX 的BOM版本, 已跳过建单";
            }
            saveBomVersionId(poId, prefix, entry.getValue(), bomVersionId);
        }
        return null;
    }

    /**
     * 查询编码前缀对应的 BOM 版本 ID
     *
     * @param prefix 商品编码前 9 位
     * @return BOM 版本 ID；无数据或无 GA/GX 记录时返回 null
     */
    private String queryBomVersionId(String prefix) {
        CrmCustomObjectQueryRequest request = new CrmCustomObjectQueryRequest();
        request.setObjectKey(HeartBomConstants.OBJECT_KEY_BOM);
        request.setPage(HeartBomConstants.DEFAULT_PAGE);
        request.setRows(HeartBomConstants.DEFAULT_ROWS);
        request.setQueryGroup(List.of(
                CrmQueryGroup.builder()
                        .connector(HeartBomConstants.CONNECTOR_AND)
                        .filters(List.of(CrmQueryFilter.builder()
                                .fieldKey(HeartBomConstants.FIELD_PRODUCT_CODE)
                                .operator(HeartPortConstants.OPERATOR_LIKE)
                                .fieldValues(List.of(prefix))
                                .build()))
                        .build(),
                CrmQueryGroup.builder()
                        .connector(HeartBomConstants.CONNECTOR_AND)
                        .filters(List.of(CrmQueryFilter.builder()
                                .fieldKey(HeartBomConstants.FIELD_BOM_NAME)
                                .operator(HeartPortConstants.OPERATOR_LIKE)
                                .fieldValues(List.of(HeartBomConstants.KEYWORD_MANUFACTURER))
                                .build()))
                        .build()));

        log.info("【{}】查询BOM版本, prefix={}", HeartBomLogConstants.LOG_BIZ, prefix);
        CrmCustomObjectQueryResponse response;
        try {
            response = crmCustomObjectService.queryObjectData(request);
        } catch (BizException e) {
            log.error("【{}】BOM版本查询失败, prefix={}, reason={}", HeartBomLogConstants.LOG_BIZ, prefix, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "BOM版本查询调用失败: " + e.getMessage(), e);
        }
        List<Map<String, Object>> records = response.getResponseData();
        if (records == null || records.isEmpty()) {
            log.info("【{}】BOM版本解析终止, reason=未查询到记录, prefix={}", HeartBomLogConstants.LOG_BIZ, prefix);
            return null;
        }
        for (Map<String, Object> record : records) {
            String name = toText(record.get(HeartBomConstants.FIELD_BOM_NAME));
            if (!containsRequiredKeyword(name)) {
                continue;
            }
            String id = toText(record.get(HeartPortConstants.RESPONSE_FIELD_ID));
            if (!StringUtils.hasText(id)) {
                log.info("【{}】BOM版本解析终止, reason=命中记录缺少id, prefix={}, name={}",
                        HeartBomLogConstants.LOG_BIZ, prefix, name);
                return null;
            }
            log.info("【{}】BOM版本解析成功, prefix={}, name={}, id={}",
                    HeartBomLogConstants.LOG_BIZ, prefix, name, id);
            return id;
        }
        log.info("【{}】BOM版本解析终止, reason=记录均不含GA/GX, prefix={}, count={}",
                HeartBomLogConstants.LOG_BIZ, prefix, records.size());
        return null;
    }

    /**
     * 回写明细 BOM 版本（仅填充仍为空的记录）
     *
     * @param poId         PO 主键
     * @param prefix       商品编码前缀
     * @param items        该前缀下的明细
     * @param bomVersionId BOM 版本 ID
     */
    private void saveBomVersionId(Long poId, String prefix, List<HeartPoItemDO> items, String bomVersionId) {
        List<Long> ids = items.stream().map(HeartPoItemDO::getId).toList();
        int rows = heartPoItemMapper.update(null, Wrappers.<HeartPoItemDO>lambdaUpdate()
                .in(HeartPoItemDO::getId, ids)
                .and(w -> w.isNull(HeartPoItemDO::getBomVersionId).or().eq(HeartPoItemDO::getBomVersionId, ""))
                .set(HeartPoItemDO::getBomVersionId, bomVersionId)
                .set(HeartPoItemDO::getUpdatedAt, LocalDateTime.now()));
        log.info("【{}】回写BOM版本, poId={}, prefix={}, bomVersionId={}, rows={}",
                HeartBomLogConstants.LOG_BIZ, poId, prefix, bomVersionId, rows);
    }

    /**
     * 判断名称是否包含任一版本关键字（忽略大小写）
     *
     * @param name 名称
     * @return true 表示命中
     */
    private boolean containsRequiredKeyword(String name) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        return HeartBomConstants.REQUIRED_NAME_KEYWORDS.stream().anyMatch(upper::contains);
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
                    Object nested = map.get(key);
                    if (nested != null) {
                        return String.valueOf(nested);
                    }
                }
                return null;
            }
            case List<?> list -> {
                return list.isEmpty() ? null : toText(list.getFirst());
            }
            default -> {
                return String.valueOf(value);
            }
        }
    }
}
