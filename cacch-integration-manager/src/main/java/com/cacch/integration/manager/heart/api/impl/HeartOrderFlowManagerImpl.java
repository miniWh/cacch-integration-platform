package com.cacch.integration.manager.heart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.constant.heart.HeartOrderConstants;
import com.cacch.integration.common.constant.heart.HeartOrderLogConstants;
import com.cacch.integration.common.dto.heart.HeartOrderSubmitResult;
import com.cacch.integration.entity.heart.HeartPoDO;
import com.cacch.integration.mapper.heart.HeartPoMapper;
import com.cacch.integration.manager.heart.api.IHeartOrderFlowManager;
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

    @Override
    public HeartOrderSubmitResult submitOrder() {
        log.info("【{}】收到 ESB Heart 订单提交请求", HeartOrderLogConstants.LOG_BIZ);
        HeartPoDO pending = heartPoMapper.selectOne(Wrappers.<HeartPoDO>lambdaQuery()
                .and(wrapper -> wrapper.isNull(HeartPoDO::getCacPoNo).or().eq(HeartPoDO::getCacPoNo, ""))
                .orderByAsc(HeartPoDO::getCreatedAt)
                .last("LIMIT 1"));
        if (pending == null) {
            log.info("【{}】无待取号 PO, 跳过", HeartOrderLogConstants.LOG_BIZ);
            return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_NO_DATA, "无待取号 PO", null);
        }
        String cacPoNo = heartSerialNoService.acquireCacPoNo(pending.getId());
        log.info("【{}】CAC PO 取号完成, poId={}, cacPoNo={}", HeartOrderLogConstants.LOG_BIZ, pending.getId(), cacPoNo);
        return new HeartOrderSubmitResult(HeartOrderConstants.SUBMIT_STATUS_SUCCESS,
                "取号成功, cacPoNo=" + cacPoNo, cacPoNo);
    }
}
