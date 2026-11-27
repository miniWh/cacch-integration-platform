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
        HeartPoDO pending = selectPendingPo();
        if (pending == null) {
            log.info("【{}】无待处理 PO, 跳过", HeartOrderLogConstants.LOG_BIZ);
            return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_NO_DATA, "无待处理 PO", null, null);
        }
        Long poId = pending.getId();
        String poNo = pending.getPoNo();
        String cacPoNo = acquireCacPoNo(pending);
        // 港口未解析时执行附件上传与港口解析；已解析（含建单认领超时回到 CREATING）的 PO 直接进入后续步骤
        if (needResolvePorts(pending)) {
            resolveAttachmentAndPorts(poId, poNo);
        }
        String bomSkipReason = resolveBomVersion(poId, poNo);
        if (bomSkipReason != null) {
            return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_FAILED, bomSkipReason, cacPoNo, null);
        }
        return createContract(poId, poNo, cacPoNo);
    }

    /**
     * 查询最早创建的一条待处理 PO
     *
     * <p>待处理范围：待取号、待港口解析、待建单、建单认领超时。</p>
     *
     * @return 待处理 PO，无数据时返回 {@code null}
     */
    private HeartPoDO selectPendingPo() {
        return heartPoMapper.selectOne(Wrappers.<HeartPoDO>lambdaQuery()
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
    }

    /**
     * 获取 CAC PO 号，已有号时直接复用
     *
     * @param po PO 主表
     * @return CAC PO 号
     */
    private String acquireCacPoNo(HeartPoDO po) {
        String cacPoNo = po.getCacPoNo();
        if (!StringUtils.hasText(cacPoNo)) {
            cacPoNo = heartSerialNoService.acquireCacPoNo(po.getId());
            log.info("【{}】CAC PO 取号完成, poId={}, poNo={}, cacPoNo={}",
                    HeartOrderLogConstants.LOG_BIZ, po.getId(), po.getPoNo(), cacPoNo);
        }
        return cacPoNo;
    }

    /**
     * 判断是否需要执行附件上传与港口解析
     *
     * @param po PO 主表
     * @return 是否需要解析
     */
    private boolean needResolvePorts(HeartPoDO po) {
        return !HeartPoProcessStatusConstants.PORT_RESOLVED.equals(po.getProcessStatus())
                && !HeartPoProcessStatusConstants.CREATING.equals(po.getProcessStatus());
    }

    /**
     * 上传附件并解析港口对象 ID
     *
     * @param poId PO 主键
     * @param poNo PO 单号
     */
    private void resolveAttachmentAndPorts(Long poId, String poNo) {
        // 附件上传并按需落盘 NAS，已有 URL 时跳过
        heartAttachmentService.resolveAndSaveAttachment(poId);
        log.info("【{}】PO 附件上传完成, poId={}, poNo={}", HeartOrderLogConstants.LOG_BIZ, poId, poNo);
        // 港口解析并回写对象 ID，成功后推进 PORT_RESOLVED
        heartPortService.resolveAndSavePorts(poId);
        log.info("【{}】港口对象ID回写完成, poId={}, poNo={}", HeartOrderLogConstants.LOG_BIZ, poId, poNo);
    }

    /**
     * 解析 BOM 版本并回写明细
     *
     * <p>无法确定版本时不改变状态，仅追加备注，便于人工修正后自动重试。</p>
     *
     * @param poId PO 主键
     * @param poNo PO 单号
     * @return 跳过原因，解析成功返回 {@code null}
     */
    private String resolveBomVersion(Long poId, String poNo) {
        String bomSkipReason = heartBomVersionService.resolveAndSaveBomVersions(poId);
        if (bomSkipReason != null) {
            appendRemark(poId, bomSkipReason);
            log.info("【{}】BOM版本未确定, 跳过建单, poId={}, poNo={}, reason={}",
                    HeartOrderLogConstants.LOG_BIZ, poId, poNo, bomSkipReason);
            return bomSkipReason;
        }
        log.info("【{}】BOM版本解析完成, poId={}, poNo={}", HeartOrderLogConstants.LOG_BIZ, poId, poNo);
        return null;
    }

    /**
     * 调用 CRM 建单并封装流程结果
     *
     * @param poId    PO 主键
     * @param poNo    PO 单号
     * @param cacPoNo CAC PO 号
     * @return 提交结果
     */
    private HeartOrderSubmitResult createContract(Long poId, String poNo, String cacPoNo) {
        // 服务内部完成幂等校验、报文组装、提交记录落库与状态推进
        HeartOrderCreateResult createResult = heartOrderCreateService.createOrder(poId);
        if (!createResult.success()) {
            log.error("【{}】CRM 建单失败, poId={}, poNo={}, message={}",
                    HeartOrderLogConstants.LOG_BIZ, poId, poNo, createResult.message());
            return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_FAILED,
                    createResult.message(), cacPoNo, null);
        }
        log.info("【{}】CRM 建单完成, poId={}, poNo={}, crmContractNo={}, skipped={}",
                HeartOrderLogConstants.LOG_BIZ, poId, poNo, createResult.crmContractNo(), createResult.skipped());
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
