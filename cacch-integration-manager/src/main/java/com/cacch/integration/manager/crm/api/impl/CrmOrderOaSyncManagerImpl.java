package com.cacch.integration.manager.crm.api.impl;

import com.cacch.integration.common.config.crm.CrmSyncProperties;
import com.cacch.integration.common.config.oa.OaProperties;
import com.cacch.integration.common.constant.crm.CrmOaFormConstants;
import com.cacch.integration.common.constant.oa.OaConstants;
import com.cacch.integration.common.dto.crm.CrmOaUserMappingResult;
import com.cacch.integration.common.dto.crm.CrmOrderOaSyncResult;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.common.enums.crm.CrmDetailFetchStatusEnum;
import com.cacch.integration.common.enums.crm.CrmOaSyncStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.crm.CrmOrderDO;
import com.cacch.integration.entity.crm.CrmOrderDetailDO;
import com.cacch.integration.integration.crm.support.CrmOrderPayloadSupport;
import com.cacch.integration.integration.oa.client.dto.OaProcessStartRequest;
import com.cacch.integration.integration.oa.support.OaResponseSupport;
import com.cacch.integration.manager.crm.api.ICrmOaUserMappingManager;
import com.cacch.integration.manager.crm.api.ICrmOrderOaSyncManager;
import com.cacch.integration.manager.crm.support.CrmOaFormMappingSupport;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import com.cacch.integration.service.crm.api.ICrmOrderDetailService;
import com.cacch.integration.service.crm.api.ICrmOrderService;
import com.cacch.integration.service.oa.api.IOaOpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * CRM 订单 OA 表单同步编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmOrderOaSyncManagerImpl implements ICrmOrderOaSyncManager {

    private static final String BIZ = "crm";

    private final ICrmOrderService crmOrderService;
    private final ICrmOrderDetailService crmOrderDetailService;
    private final ICrmOaUserMappingManager crmOaUserMappingManager;
    private final IOaOpenApiService oaOpenApiService;
    private final IWeComWebhookManager weComWebhookManager;
    private final CrmSyncProperties crmSyncProperties;
    private final OaProperties oaProperties;

    @Override
    public CrmOrderOaSyncResult syncPendingDetails() {
        int batchSize = Math.max(1, crmSyncProperties.getBatchSize());
        int maxRetry = crmSyncProperties.getMaxRetry() > 0
                ? crmSyncProperties.getMaxRetry()
                : CrmOaFormConstants.DEFAULT_MAX_RETRY;

        List<CrmOrderDetailDO> details = crmOrderDetailService.listPendingOrRetry(batchSize, maxRetry);
        log.info("【{}】开始OA同步, scanned={}, batchSize={}, maxRetry={}",
                CrmOaFormConstants.LOG_BIZ, details.size(), batchSize, maxRetry);

        int success = 0;
        int retry = 0;
        int failed = 0;
        int skipped = 0;

        for (CrmOrderDetailDO detail : details) {
            try {
                String outcome = syncOne(detail, maxRetry);
                if (CrmOaSyncStatusEnum.SUCCESS.getCode().equals(outcome)) {
                    success++;
                } else if (CrmOaSyncStatusEnum.FAILED.getCode().equals(outcome)) {
                    failed++;
                } else if (CrmOaSyncStatusEnum.RETRY.getCode().equals(outcome)) {
                    retry++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                log.info("【{}】单条同步异常终止, detailId={}, crmDetailId={}, reason={}",
                        CrmOaFormConstants.LOG_BIZ, detail.getId(), detail.getCrmDetailId(), e.getMessage());
                log.error("【{}】单条同步失败, detailId={}, crmDetailId={}",
                        CrmOaFormConstants.LOG_BIZ, detail.getId(), detail.getCrmDetailId(), e);
                String status = crmOrderDetailService.markOaSyncFailure(
                        detail.getId(), "同步异常: " + e.getMessage(), maxRetry);
                if (CrmOaSyncStatusEnum.FAILED.getCode().equals(status)) {
                    failed++;
                    alertFailed(detail, "同步异常: " + e.getMessage());
                } else {
                    retry++;
                }
            }
        }

        CrmOrderOaSyncResult result = CrmOrderOaSyncResult.builder()
                .scanned(details.size())
                .success(success)
                .retry(retry)
                .failed(failed)
                .skipped(skipped)
                .build();
        log.info("【{}】本轮OA同步完成, scanned={}, success={}, retry={}, failed={}, skipped={}",
                CrmOaFormConstants.LOG_BIZ, result.getScanned(), result.getSuccess(),
                result.getRetry(), result.getFailed(), result.getSkipped());
        return result;
    }

    /**
     * 同步单条明细
     *
     * @param detail   明细
     * @param maxRetry 最大重试次数
     * @return 结果状态码：SUCCESS / RETRY / FAILED / SKIPPED
     */
    private String syncOne(CrmOrderDetailDO detail, int maxRetry) {
        if (detail == null || detail.getId() == null) {
            log.info("【{}】跳过同步, reason=明细为空", CrmOaFormConstants.LOG_BIZ);
            return "SKIPPED";
        }

        CrmOrderDO order = crmOrderService.getById(detail.getOrderId());
        if (order == null) {
            log.info("【{}】跳过同步, reason=主表不存在, detailId={}, orderId={}",
                    CrmOaFormConstants.LOG_BIZ, detail.getId(), detail.getOrderId());
            return "SKIPPED";
        }
        if (!CrmDetailFetchStatusEnum.SUCCESS.getCode().equals(order.getDetailFetchStatus())) {
            log.info("【{}】跳过同步, reason=明细未拉取成功, detailId={}, orderId={}, detailFetchStatus={}",
                    CrmOaFormConstants.LOG_BIZ, detail.getId(), order.getId(), order.getDetailFetchStatus());
            return "SKIPPED";
        }

        String creatorEmployeeId = resolveCreatorEmployeeId(order);
        if (!StringUtils.hasText(creatorEmployeeId)) {
            String err = "订单创建人CRM员工ID为空(creator_id.id)";
            log.info("【{}】人员映射终止, detailId={}, crmDetailId={}, reason={}",
                    CrmOaFormConstants.LOG_BIZ, detail.getId(), detail.getCrmDetailId(), err);
            String status = crmOrderDetailService.markOaSyncFailure(detail.getId(), err, maxRetry);
            if (CrmOaSyncStatusEnum.FAILED.getCode().equals(status)) {
                alertFailed(detail, err);
            }
            return status;
        }
        CrmOaUserMappingResult mapping = crmOaUserMappingManager.resolve(creatorEmployeeId);
        if (!mapping.isSuccess() || !StringUtils.hasText(mapping.getOaUserId())) {
            String err = StringUtils.hasText(mapping.getErrorMessage())
                    ? mapping.getErrorMessage()
                    : "人员映射失败";
            log.info("【{}】人员映射失败, detailId={}, crmDetailId={}, creatorEmployeeId={}, reason={}",
                    CrmOaFormConstants.LOG_BIZ, detail.getId(), detail.getCrmDetailId(),
                    creatorEmployeeId, err);
            String status = crmOrderDetailService.markOaSyncFailure(detail.getId(), err, maxRetry);
            if (CrmOaSyncStatusEnum.FAILED.getCode().equals(status)) {
                alertFailed(detail, err);
            }
            return status;
        }

        Map<String, Object> formmain = CrmOaFormMappingSupport.buildFormMain(order, mapping.getOaUserId());
        Map<String, Object> formson = CrmOaFormMappingSupport.buildFormSon(order, detail);

        String loginName = StringUtils.hasText(mapping.getOaLoginName())
                ? mapping.getOaLoginName()
                : oaProperties.getDefaultLoginName();
        OaProcessStartRequest request = OaProcessStartRequest.builder()
                .loginName(loginName)
                .appName(OaConstants.APP_NAME_COLLABORATION)
                .templateCode(oaProperties.getTemplateCode())
                .draft(OaConstants.DRAFT_SEND)
                .formmain2817(formmain)
                .formson2819(formson)
                .build();

        try {
            JsonNode response = oaOpenApiService.startProcess(request);
            String processId = OaResponseSupport.extractProcessId(response);
            if (!StringUtils.hasText(processId)) {
                log.info("【{}】发起OA成功但未解析到流程ID, detailId={}, crmDetailId={}",
                        CrmOaFormConstants.LOG_BIZ, detail.getId(), detail.getCrmDetailId());
                String status = crmOrderDetailService.markOaSyncFailure(
                        detail.getId(), "OA响应未解析到流程实例ID", maxRetry);
                if (CrmOaSyncStatusEnum.FAILED.getCode().equals(status)) {
                    alertFailed(detail, "OA响应未解析到流程实例ID");
                }
                return status;
            }
            crmOrderDetailService.markOaSyncSuccess(detail.getId(), processId);
            log.info("【{}】明细同步成功, detailId={}, crmDetailId={}, orderNo={}, oaProcessId={}",
                    CrmOaFormConstants.LOG_BIZ, detail.getId(), detail.getCrmDetailId(),
                    detail.getOrderNo(), processId);
            return CrmOaSyncStatusEnum.SUCCESS.getCode();
        } catch (BizException e) {
            log.info("【{}】发起OA业务失败, detailId={}, crmDetailId={}, reason={}",
                    CrmOaFormConstants.LOG_BIZ, detail.getId(), detail.getCrmDetailId(), e.getMessage());
            String status = crmOrderDetailService.markOaSyncFailure(detail.getId(), e.getMessage(), maxRetry);
            if (CrmOaSyncStatusEnum.FAILED.getCode().equals(status)) {
                alertFailed(detail, e.getMessage());
            }
            return status;
        }
    }

    /**
     * 从订单原始报文解析创建人 CRM 员工 ID（{@code creator_id.id}）
     *
     * @param order 订单主表
     * @return 员工 ID；无法解析时返回 null
     */
    private String resolveCreatorEmployeeId(CrmOrderDO order) {
        JsonNode raw = CrmOrderPayloadSupport.asJsonNode(order.getRawPayload());
        return CrmOrderPayloadSupport.nestedText(raw, "creator_id", "id");
    }

    private void alertFailed(CrmOrderDetailDO detail, String errorMessage) {
        weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                .biz(BIZ)
                .title("CRM订单OA同步失败需人工介入")
                .subject("明细 " + detail.getCrmDetailId())
                .context("orderNo=" + detail.getOrderNo() + ", detailId=" + detail.getId())
                .errorMessage(errorMessage)
                .dedupType("crm-oa-sync")
                .dedupId(String.valueOf(detail.getId()))
                .mention(true)
                .build());
    }
}
