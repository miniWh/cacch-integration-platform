package com.cacch.integration.controller.heart;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.heart.vo.HeartOrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Heart 客户 PO 订单对外接口（供 ESB 调用）
 *
 * <p>无入参，邮箱、附件、CRM 等参数全部取平台配置（{@code heart.*}）。
 * 当前为空实现：仅打通调用链路并固定契约，不落库、不触发业务流程，
 * 待 3.1 功能点阶段一（邮件接入）实现后接入状态机。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/heart/orders")
public class HeartOrderOpenApiController {

    /**
     * 触发 Heart PO 订单自动化处理
     *
     * <p>空实现：统一返回 {@code PENDING}，业务逻辑待实现。
     *
     * @return 受理结果
     */
    @PostMapping("/submit")
    public Result<HeartOrderSubmitVO> submit() {
        log.info("收到 ESB Heart 订单提交请求");
        return Result.success(new HeartOrderSubmitVO("PENDING", "接口已联通，业务逻辑待实现"));
    }
}
