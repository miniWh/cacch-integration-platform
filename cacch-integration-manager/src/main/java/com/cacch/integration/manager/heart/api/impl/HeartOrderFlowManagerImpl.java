package com.cacch.integration.manager.heart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.constant.heart.HeartOrderConstants;
import com.cacch.integration.common.constant.heart.HeartOrderLogConstants;
import com.cacch.integration.common.constant.heart.HeartPoProcessStatusConstants;
import com.cacch.integration.common.dto.heart.HeartOrderSubmitResult;
import com.cacch.integration.entity.heart.HeartPoDO;
import com.cacch.integration.mapper.heart.HeartPoMapper;
import com.cacch.integration.manager.heart.api.IHeartOrderFlowManager;
import com.cacch.integration.service.heart.api.IHeartPortService;
import com.cacch.integration.service.heart.api.IHeartSerialNoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    @Override
    public HeartOrderSubmitResult submitOrder() {
        log.info("【{}】收到 ESB Heart 订单提交请求", HeartOrderLogConstants.LOG_BIZ);
        HeartPoDO pending = heartPoMapper.selectOne(Wrappers.<HeartPoDO>lambdaQuery()
                .and(wrapper -> wrapper
                        // 待取号（客户已解析且无 CAC PO 号）
                        .nested(w -> w.eq(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.CUSTOMER_RESOLVED)
                                .and(cac -> cac.isNull(HeartPoDO::getCacPoNo).or().eq(HeartPoDO::getCacPoNo, "")))
                        // 已取号但港口对象 ID 未回写（取号成功、港口解析失败后的重试）
                        .or()
                        .nested(w -> w.eq(HeartPoDO::getProcessStatus, HeartPoProcessStatusConstants.SERIAL_NUMBERED)
                                .and(port -> port.isNull(HeartPoDO::getPortOfLoadingId)
                                        .or().eq(HeartPoDO::getPortOfLoadingId, "")
                                        .or().isNull(HeartPoDO::getPortOfDischargeId)
                                        .or().eq(HeartPoDO::getPortOfDischargeId, ""))))
                .orderByAsc(HeartPoDO::getCreatedAt)
                .last("LIMIT 1"));
        if (pending == null) {
            log.info("【{}】无待处理 PO, 跳过", HeartOrderLogConstants.LOG_BIZ);
            return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_NO_DATA, "无待处理 PO", null);
        }
        // 取号（已有 CAC PO 号时服务内部直接复用，重试不会重复取号）
        String cacPoNo = heartSerialNoService.acquireCacPoNo(pending.getId());
        log.info("【{}】CAC PO 取号完成, poId={}, poNo={}, cacPoNo={}",
                HeartOrderLogConstants.LOG_BIZ, pending.getId(), pending.getPoNo(), cacPoNo);
        // 港口解析并回写对象 ID，失败后 PO 保持 SERIAL_NUMBERED，下次提交只重试本阶段
        heartPortService.resolveAndSavePorts(pending.getId());
        log.info("【{}】港口对象ID回写完成, poId={}, poNo={}",
                HeartOrderLogConstants.LOG_BIZ, pending.getId(), pending.getPoNo());
        return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_SUCCESS,
                "取号及港口解析成功, poNo=" + pending.getPoNo() + ", cacPoNo=" + cacPoNo, cacPoNo);
    }
}
