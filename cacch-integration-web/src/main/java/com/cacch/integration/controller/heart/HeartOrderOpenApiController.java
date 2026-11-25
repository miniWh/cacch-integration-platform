package com.cacch.integration.controller.heart;

import com.cacch.integration.common.dto.heart.HeartOrderSubmitResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.heart.vo.HeartOrderSubmitVO;
import com.cacch.integration.manager.heart.api.IHeartOrderFlowManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * @return 受理结果
     */
    @PostMapping("/submit")
    public Result<HeartOrderSubmitVO> submit() {
        HeartOrderSubmitResult result = heartOrderFlowManager.submitOrder();
        return Result.success(new HeartOrderSubmitVO(result.status(), result.message(), result.crmContractNo()));
    }
}
