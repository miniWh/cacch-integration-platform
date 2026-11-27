package com.cacch.integration.manager.heart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.constant.heart.HeartOrderConstants;
import com.cacch.integration.common.constant.heart.HeartOrderLogConstants;
import com.cacch.integration.common.constant.heart.HeartPoProcessStatusConstants;
import com.cacch.integration.common.dto.heart.HeartMailPullResult;
import com.cacch.integration.common.dto.heart.HeartOrderBatchSubmitResult;
import com.cacch.integration.common.dto.heart.HeartOrderCreateResult;
import com.cacch.integration.common.dto.heart.HeartOrderItemSubmitResult;
import com.cacch.integration.common.dto.heart.HeartOrderSubmitResult;
import com.cacch.integration.entity.heart.HeartPoDO;
import com.cacch.integration.mapper.heart.HeartPoMapper;
import com.cacch.integration.manager.heart.api.IHeartOrderFlowManager;
import com.cacch.integration.service.heart.api.IHeartAttachmentService;
import com.cacch.integration.service.heart.api.IHeartBomVersionService;
import com.cacch.integration.service.heart.api.IHeartMailService;
import com.cacch.integration.service.heart.api.IHeartOrderCreateService;
import com.cacch.integration.service.heart.api.IHeartPortService;
import com.cacch.integration.service.heart.api.IHeartSerialNoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Heart 客户 PO 订单流程编排实现
 *
 * <p>一次调用按「认领 → 取号 → 附件/港口 → BOM → 建单」串行处理多条待处理 PO，
 * 单轮最多 {@code SUBMIT_MAX_PER_ROUND} 条，单条失败不影响其余记录。</p>
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

    private final IHeartMailService heartMailService;

    @Override
    public HeartOrderBatchSubmitResult submitOrder() {
        log.info("【{}】收到 ESB Heart 订单提交请求", HeartOrderLogConstants.LOG_BIZ);
        pullMails();

        List<HeartOrderItemSubmitResult> items = new ArrayList<>();
        Set<Long> processedIds = new HashSet<>();
        int successCount = 0;
        int failedCount = 0;
        while (items.size() < HeartOrderConstants.SUBMIT_MAX_PER_ROUND) {
            HeartPoDO pending = selectPendingPo(processedIds);
            if (pending == null) {
                break;
            }
            Long poId = pending.getId();
            // 已处理的 PO 不再重复取到，避免失败记录（状态未变）在同一轮内反复占位
            processedIds.add(poId);
            String token = UUID.randomUUID().toString();
            if (!claim(poId, token)) {
                log.info("【{}】PO 已被其他请求认领, 跳过, poId={}, poNo={}",
                        HeartOrderLogConstants.LOG_BIZ, poId, pending.getPoNo());
                continue;
            }
            HeartOrderItemSubmitResult item = processOne(pending, token);
            items.add(item);
            if (HeartOrderConstants.SUBMIT_STATUS_SUCCESS.equals(item.status())) {
                successCount++;
            } else {
                failedCount++;
            }
        }
        boolean hasMore = items.size() >= HeartOrderConstants.SUBMIT_MAX_PER_ROUND
                && selectPendingPo(processedIds) != null;
        return buildBatchResult(items, successCount, failedCount, hasMore);
    }

    /**
     * 拉取当天邮件并入库（含失败邮件重试）
     *
     * <p>开始/结束时间为空时由 Service 兜底为当天 {@code 00:00:00} 至 {@code 23:59:59}；
     * 拉取或入库失败只记录日志，不阻塞后续已入库 PO 的建单流程。</p>
     */
    private void pullMails() {
        try {
            HeartMailPullResult result = heartMailService.pullAndSaveMails(null, null);
            log.info("【{}】邮件拉取入库完成, 拉取 {} 封, 重试 {} 封, 成功 {} 封, 跳过 {} 封, 失败 {} 封",
                    HeartOrderLogConstants.LOG_BIZ, result.getPulled(), result.getRetried(),
                    result.getSuccessCount(), result.getSkipCount(), result.getFailureCount());
        } catch (Exception e) {
            log.error("【{}】邮件拉取入库失败, 本轮继续处理已入库 PO", HeartOrderLogConstants.LOG_BIZ, e);
        }
    }

    /**
     * 查询最早创建的一条待处理 PO
     *
     * <p>待处理范围：待取号、待港口解析、待建单、建单认领超时；
     * 排除本轮已处理的 PO，以及已被认领且未超时的 PO。</p>
     *
     * @param excludeIds 本轮已处理的 PO 主键集合
     * @return 待处理 PO，无数据时返回 {@code null}
     */
    private HeartPoDO selectPendingPo(Set<Long> excludeIds) {
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
                // 未被认领，或认领已超时（进程异常中断的兜底）
                .and(w -> w.isNull(HeartPoDO::getClaimToken)
                        .or().lt(HeartPoDO::getClaimAt, LocalDateTime.now()
                                .minusMinutes(HeartOrderConstants.CREATE_CLAIM_TIMEOUT_MINUTES)))
                .notIn(!excludeIds.isEmpty(), HeartPoDO::getId, excludeIds)
                .orderByAsc(HeartPoDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    /**
     * 认领 PO（条件更新，依赖数据库行级原子性保证并发互斥）
     *
     * @param poId  PO 主键
     * @param token 认领令牌
     * @return 是否认领成功
     */
    private boolean claim(Long poId, String token) {
        LocalDateTime now = LocalDateTime.now();
        int rows = heartPoMapper.update(null, Wrappers.<HeartPoDO>lambdaUpdate()
                .eq(HeartPoDO::getId, poId)
                .and(w -> w.isNull(HeartPoDO::getClaimToken)
                        .or().lt(HeartPoDO::getClaimAt, now
                                .minusMinutes(HeartOrderConstants.CREATE_CLAIM_TIMEOUT_MINUTES)))
                .set(HeartPoDO::getClaimToken, token)
                .set(HeartPoDO::getClaimAt, now));
        return rows > 0;
    }

    /**
     * 释放认领
     *
     * <p>仅成功时释放；失败时保留认领，由认领超时实现退避重试，避免失败记录长期占用队列首位。</p>
     *
     * @param poId  PO 主键
     * @param token 认领令牌
     */
    private void releaseClaim(Long poId, String token) {
        heartPoMapper.update(null, Wrappers.<HeartPoDO>lambdaUpdate()
                .eq(HeartPoDO::getId, poId)
                .eq(HeartPoDO::getClaimToken, token)
                .set(HeartPoDO::getClaimToken, null)
                .set(HeartPoDO::getClaimAt, null));
    }

    /**
     * 处理单条 PO，异常隔离，不影响本轮其余记录
     *
     * @param po    待处理 PO
     * @param token 认领令牌
     * @return 单条处理结果
     */
    private HeartOrderItemSubmitResult processOne(HeartPoDO po, String token) {
        Long poId = po.getId();
        String poNo = po.getPoNo();
        String cacPoNo = null;
        boolean success = false;
        try {
            cacPoNo = acquireCacPoNo(po);
            // 港口未解析时执行附件上传与港口解析；已解析（含建单认领超时回到 CREATING）的 PO 直接进入后续步骤
            if (needResolvePorts(po)) {
                resolveAttachmentAndPorts(poId, poNo);
            }
            String bomSkipReason = resolveBomVersion(poId, poNo);
            if (bomSkipReason != null) {
                return new HeartOrderItemSubmitResult(poId, poNo,
                        HeartOrderConstants.SUBMIT_STATUS_FAILED, bomSkipReason, cacPoNo, null);
            }
            HeartOrderSubmitResult createResult = createContract(poId, poNo, cacPoNo);
            success = HeartOrderConstants.SUBMIT_STATUS_SUCCESS.equals(createResult.status());
            return new HeartOrderItemSubmitResult(poId, poNo, createResult.status(),
                    createResult.message(), cacPoNo, createResult.crmContractNo());
        } catch (Exception e) {
            log.error("【{}】单条处理异常, 继续处理下一条, poId={}, poNo={}",
                    HeartOrderLogConstants.LOG_BIZ, poId, poNo, e);
            return new HeartOrderItemSubmitResult(poId, poNo,
                    HeartOrderConstants.SUBMIT_STATUS_FAILED, "处理异常: " + e.getMessage(), cacPoNo, null);
        } finally {
            if (success) {
                releaseClaim(poId, token);
            } else {
                log.info("【{}】本轮未成功, 保留认领以便超时后重试, poId={}, poNo={}",
                        HeartOrderLogConstants.LOG_BIZ, poId, poNo);
            }
        }
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
     * 调用 CRM 建单并封装单条结果
     *
     * @param poId    PO 主键
     * @param poNo    PO 单号
     * @param cacPoNo CAC PO 号
     * @return 单条提交结果
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
     * 汇总本轮批量结果
     *
     * @param items        单条明细
     * @param successCount 成功条数
     * @param failedCount  失败条数
     * @param hasMore      是否仍有待处理 PO
     * @return 批量结果
     */
    private HeartOrderBatchSubmitResult buildBatchResult(List<HeartOrderItemSubmitResult> items,
                                                         int successCount, int failedCount, boolean hasMore) {
        if (items.isEmpty()) {
            log.info("【{}】无待处理 PO, 跳过", HeartOrderLogConstants.LOG_BIZ);
            return new HeartOrderBatchSubmitResult(HeartOrderConstants.SUBMIT_STATUS_NO_DATA,
                    "无待处理 PO", 0, 0, 0, false, List.of());
        }
        String status;
        if (failedCount == 0) {
            status = HeartOrderConstants.SUBMIT_STATUS_SUCCESS;
        } else if (successCount == 0) {
            status = HeartOrderConstants.SUBMIT_STATUS_FAILED;
        } else {
            status = HeartOrderConstants.SUBMIT_STATUS_PARTIAL;
        }
        String message = String.format("本轮处理%d条，成功%d条，失败%d条",
                items.size(), successCount, failedCount);
        log.info("【{}】{}, hasMore={}", HeartOrderLogConstants.LOG_BIZ, message, hasMore);
        return new HeartOrderBatchSubmitResult(status, message, items.size(),
                successCount, failedCount, hasMore, items);
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
