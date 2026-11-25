package com.cacch.integration.service.heart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.constant.heart.HeartPoProcessStatusConstants;
import com.cacch.integration.common.constant.heart.HeartSerialNoConstants;
import com.cacch.integration.common.constant.heart.HeartSerialNoLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.heart.HeartPoDO;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectSaveRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectSaveResponse;
import com.cacch.integration.mapper.heart.HeartPoMapper;
import com.cacch.integration.service.crm.api.ICrmCustomObjectService;
import com.cacch.integration.service.heart.api.IHeartSerialNoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Heart 客户 PO 取号服务实现
 *
 * <p>取号接口参数全部为固定值，统一收敛在 {@link HeartSerialNoConstants}；
 * PO 记录先由邮件入库流程落库，取号节点再调用本服务；
 * 幂等策略：先查 PO 上已有 CAC PO 号直接复用，取号后以「状态为 CUSTOMER_RESOLVED 且 cac_po_no 为空」为条件更新，
 * 成功后推进为 SERIAL_NUMBERED，并发冲突时重查并复用已写入的号码，保证同一 PO 只取号一次。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartSerialNoServiceImpl implements IHeartSerialNoService {

    private final HeartPoMapper heartPoMapper;

    private final ICrmCustomObjectService crmCustomObjectService;

    @Override
    public String acquireCacPoNo(Long poId) {
        if (poId == null) {
            log.info("【{}】CAC PO 取号终止, reason=poId为空", HeartSerialNoLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "poId 不能为空");
        }
        HeartPoDO po = heartPoMapper.selectById(poId);
        if (po == null) {
            log.info("【{}】CAC PO 取号终止, reason=PO不存在, poId={}", HeartSerialNoLogConstants.LOG_BIZ, poId);
            throw new BizException(ResultCode.PARAM_INVALID, "PO 不存在, poId=" + poId);
        }
        if (StringUtils.hasText(po.getCacPoNo())) {
            log.info("【{}】CAC PO 取号复用已有号码, poId={}, cacPoNo={}",
                    HeartSerialNoLogConstants.LOG_BIZ, poId, po.getCacPoNo());
            return po.getCacPoNo();
        }

        String cacPoNo = requestCacPoNo(poId);
        int rows = heartPoMapper.update(null, Wrappers.<HeartPoDO>lambdaUpdate()
                .eq(HeartPoDO::getId, poId)
                .eq(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.CUSTOMER_RESOLVED)
                .and(wrapper -> wrapper.isNull(HeartPoDO::getCacPoNo).or().eq(HeartPoDO::getCacPoNo, ""))
                .set(HeartPoDO::getCacPoNo, cacPoNo)
                .set(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.SERIAL_NUMBERED)
                .setSql("submit_count = COALESCE(submit_count, 0) + 1")
                .set(HeartPoDO::getLastSubmitAt, LocalDateTime.now())
                .set(HeartPoDO::getUpdatedAt, LocalDateTime.now()));
        if (rows > 0) {
            log.info("【{}】CAC PO 取号成功并推进状态, poId={}, cacPoNo={}",
                    HeartSerialNoLogConstants.LOG_BIZ, poId, cacPoNo);
            return cacPoNo;
        }

        HeartPoDO latest = heartPoMapper.selectById(poId);
        if (latest == null || !StringUtils.hasText(latest.getCacPoNo())) {
            log.info("【{}】CAC PO 取号回写失败, poId={}, cacPoNo={}", HeartSerialNoLogConstants.LOG_BIZ, poId, cacPoNo);
            throw new BizException(ResultCode.SYSTEM_ERROR, "CAC PO 号回写失败, poId=" + poId);
        }
        log.info("【{}】CAC PO 取号并发复用最新号码, poId={}, cacPoNo={}",
                HeartSerialNoLogConstants.LOG_BIZ, poId, latest.getCacPoNo());
        return latest.getCacPoNo();
    }

    /**
     * 按固定参数调用勤策自定义对象接口取号
     *
     * @param poId PO 主键，仅用于日志定位
     * @return CAC PO 流水号（response_data 数组首条记录的 id）
     */
    private String requestCacPoNo(Long poId) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(HeartSerialNoConstants.FIELD_CAC_PO_NO, HeartSerialNoConstants.CAC_PO_NO_FIELD_VALUE);
        fields.put(HeartSerialNoConstants.FIELD_OWNER, HeartSerialNoConstants.OWNER_ID);

        CrmCustomObjectSaveRequest request = new CrmCustomObjectSaveRequest();
        request.setObjectKey(HeartSerialNoConstants.OBJECT_KEY_CAC_PO);
        request.setFields(fields);
        request.setIsTrigger(HeartSerialNoConstants.IS_TRIGGER);
        request.setCheckRule(HeartSerialNoConstants.CHECK_RULE);

        log.info("【{}】发起 CAC PO 取号, poId={}, objectKey={}",
                HeartSerialNoLogConstants.LOG_BIZ, poId, request.getObjectKey());
        try {
            CrmCustomObjectSaveResponse response = crmCustomObjectService.saveObjectData(request);
            // 勤策创建接口 response_data 为新建记录数组，取首条记录的 id 作为 CAC PO 号
            JsonNode record = response.getResponseData();
            if (record != null && record.isArray()) {
                record = !record.isEmpty() ? record.get(0) : null;
            }
            String id = record == null ? null : record.path(HeartSerialNoConstants.RESPONSE_FIELD_ID).asString();
            if (!StringUtils.hasText(id)) {
                log.info("【{}】CAC PO 取号终止, reason=响应缺少id, poId={}", HeartSerialNoLogConstants.LOG_BIZ, poId);
                throw new BizException(ResultCode.INTEGRATION_ERROR, "CAC PO 取号失败, 响应缺少 id");
            }
            return id;
        } catch (BizException e) {
            log.error("【{}】CAC PO 取号终止, poId={}, reason={}", HeartSerialNoLogConstants.LOG_BIZ, poId, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "CAC PO 取号调用失败: " + e.getMessage(), e);
        }
    }
}
