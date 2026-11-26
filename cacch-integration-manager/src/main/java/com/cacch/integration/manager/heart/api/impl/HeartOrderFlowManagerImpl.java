package com.cacch.integration.manager.heart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.constant.heart.HeartOrderConstants;
import com.cacch.integration.common.constant.heart.HeartOrderLogConstants;
import com.cacch.integration.common.constant.heart.HeartPoProcessStatusConstants;
import com.cacch.integration.common.dto.heart.HeartOrderCreateResult;
import com.cacch.integration.common.dto.heart.HeartOrderSubmitResult;
import com.cacch.integration.entity.heart.HeartPoDO;
import com.cacch.integration.mapper.heart.HeartPoMapper;
import com.cacch.integration.manager.heart.api.IHeartOrderFlowManager;
import com.cacch.integration.service.heart.api.IHeartAttachmentService;
import com.cacch.integration.service.heart.api.IHeartBomVersionService;
import com.cacch.integration.service.heart.api.IHeartOrderCreateService;
import com.cacch.integration.service.heart.api.IHeartPortService;
import com.cacch.integration.service.heart.api.IHeartSerialNoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Heart 客户 PO 订单流程编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartOrderFlowManagerImpl implements IHeartOrderFlowManager {

    private final HeartPoMapper heartPoMapper;

    private final IHeartSerialNoService heartSerialNoService;

    private final IHeartPortService heartPortService;

    private final IHeartAttachmentService heartAttachmentService;

    private final IHeartBomVersionService heartBomVersionService;

    private final IHeartOrderCreateService heartOrderCreateService;

    @Override
    public HeartOrderSubmitResult submitOrder() {
        log.info("【{}】收到 ESB Heart 订单提交请求", HeartOrderLogConstants.LOG_BIZ);
        HeartPoDO pending = heartPoMapper.selectOne(Wrappers.<HeartPoDO>lambdaQuery()
                .and(wrapper -> wrapper
                        // 待取号（客户已解析且无 CAC PO 号）
                        .nested(w -> w.eq(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.CUSTOMER_RESOLVED)
                                .and(cac -> cac.isNull(HeartPoDO::getCacPoNo).or().eq(HeartPoDO::getCacPoNo, "")))
                        // 已取号、待港口解析（港口解析失败后的重试）
                        .or()
                        .eq(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.SERIAL_NUMBERED)
                        // 港口已解析、待 CRM 建单
                        .or()
                        .eq(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.PORT_RESOLVED)
                        // 建单认领超时（进程异常中断），允许重新进入建单
                        .or()
                        .nested(w -> w.eq(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.CREATING)
                                .lt(HeartPoDO::getLastSubmitAt, LocalDateTime.now()
                                        .minusMinutes(HeartOrderConstants.CREATE_CLAIM_TIMEOUT_MINUTES))))
                .orderByAsc(HeartPoDO::getCreatedAt)
                .last("LIMIT 1"));
        if (pending == null) {
            log.info("【{}】无待处理 PO, 跳过", HeartOrderLogConstants.LOG_BIZ);
            return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_NO_DATA, "无待处理 PO", null, null);
        }
        // 取号（已有 CAC PO 号时直接复用，重试不会重复取号）
        String cacPoNo = pending.getCacPoNo();
        if (!StringUtils.hasText(cacPoNo)) {
            cacPoNo = heartSerialNoService.acquireCacPoNo(pending.getId());
            log.info("【{}】CAC PO 取号完成, poId={}, poNo={}, cacPoNo={}",
                    HeartOrderLogConstants.LOG_BIZ, pending.getId(), pending.getPoNo(), cacPoNo);
        }
        // 附件上传并按需落盘 NAS，已有 URL 时跳过
        if (!HeartPoProcessStatusConstants.PORT_RESOLVED.equals(pending.getProcessStatus())
                && !HeartPoProcessStatusConstants.CREATING.equals(pending.getProcessStatus())) {
            heartAttachmentService.resolveAndSaveAttachment(pending.getId());
            log.info("【{}】PO 附件上传完成, poId={}, poNo={}",
                    HeartOrderLogConstants.LOG_BIZ, pending.getId(), pending.getPoNo());
            // 港口解析并回写对象 ID，成功后推进 PORT_RESOLVED；已解析（含建单认领超时回到 CREATING）的 PO 直接进入建单
            heartPortService.resolveAndSavePorts(pending.getId());
            log.info("【{}】港口对象ID回写完成, poId={}, poNo={}",
                    HeartOrderLogConstants.LOG_BIZ, pending.getId(), pending.getPoNo());
        }
        // BOM 版本解析：无法确定时不改变状态，仅追加备注，便于人工修正后自动重试
        String bomSkipReason = heartBomVersionService.resolveAndSaveBomVersions(pending.getId());
        if (bomSkipReason != null) {
            appendRemark(pending.getId(), bomSkipReason);
            log.info("【{}】BOM版本未确定, 跳过建单, poId={}, poNo={}, reason={}",
                    HeartOrderLogConstants.LOG_BIZ, pending.getId(), pending.getPoNo(), bomSkipReason);
            return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_FAILED,
                    bomSkipReason, cacPoNo, null);
        }
        log.info("【{}】BOM版本解析完成, poId={}, poNo={}",
                HeartOrderLogConstants.LOG_BIZ, pending.getId(), pending.getPoNo());
        // CRM 建单：服务内部完成幂等校验、报文组装、提交记录落库与状态推进
        HeartOrderCreateResult createResult = heartOrderCreateService.createOrder(pending.getId());
        if (!createResult.success()) {
            log.error("【{}】CRM 建单失败, poId={}, poNo={}, message={}",
                    HeartOrderLogConstants.LOG_BIZ, pending.getId(), pending.getPoNo(), createResult.message());
            return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_FAILED,
                    createResult.message(), cacPoNo, null);
        }
        log.info("【{}】CRM 建单完成, poId={}, poNo={}, crmContractNo={}, skipped={}",
                HeartOrderLogConstants.LOG_BIZ, pending.getId(), pending.getPoNo(),
                createResult.crmContractNo(), createResult.skipped());
        return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_SUCCESS,
                createResult.message(), cacPoNo, createResult.crmContractNo());
    }

    /**
     * 追加主表备注，保留原有内容
     *
     * @param poId PO 主键
     * @param note 追加说明
     */
    private void appendRemark(Long poId, String note) {
        HeartPoDO latest = heartPoMapper.selectById(poId);
        String existing = latest == null ? null : latest.getRemark();
        if (StringUtils.hasText(existing) && existing.contains(note)) {
            return;
        }
        String remark = StringUtils.hasText(existing) ? existing + "；" + note : note;
        heartPoMapper.update(null, Wrappers.<HeartPoDO>lambdaUpdate()
                .eq(HeartPoDO::getId, poId)
                .set(HeartPoDO::getRemark, remark)
                .set(HeartPoDO::getUpdatedAt, LocalDateTime.now()));
    }
}
