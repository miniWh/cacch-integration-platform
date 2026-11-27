package com.cacch.integration.controller.heart;

import com.cacch.integration.common.dto.heart.HeartOrderBatchSubmitResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.heart.vo.HeartOrderBatchSubmitVO;
import com.cacch.integration.dto.heart.vo.HeartOrderItemSubmitVO;
import com.cacch.integration.manager.heart.api.IHeartOrderFlowManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Heart 客户 PO 订单对外接口
 *
 * <p>无入参，邮箱、附件、CRM 等参数全部取平台配置（{@code heart.*}）。
 * 业务流程统一由 {@link IHeartOrderFlowManager} 编排，Controller 只做转发与返回体转换。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/heart/orders")
@RequiredArgsConstructor
public class HeartOrderOpenApiController {

    private final IHeartOrderFlowManager heartOrderFlowManager;

    /**
     * 触发 Heart PO 订单自动化处理
     *
     * <p>单次调用最多处理 {@code HeartOrderConstants.SUBMIT_MAX_PER_ROUND} 条待处理 PO，
     * 返回整体状态与单条明细；{@code hasMore=true} 时建议立即再次调用以消化积压。</p>
     *
     * @return 批量受理结果
     */
    @PostMapping("/submit")
    public Result<HeartOrderBatchSubmitVO> submit() {
        HeartOrderBatchSubmitResult result = heartOrderFlowManager.submitOrder();
        List<HeartOrderItemSubmitVO> items = result.items().stream()
                .map(item -> new HeartOrderItemSubmitVO(item.poId(), item.poNo(), item.status(),
                        item.message(), item.cacPoNo(), item.crmContractNo()))
                .toList();
        return Result.success(new HeartOrderBatchSubmitVO(result.status(), result.message(), result.total(),
                result.successCount(), result.failedCount(), result.hasMore(), items));
    }
}
