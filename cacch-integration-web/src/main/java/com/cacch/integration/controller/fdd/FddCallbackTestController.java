package com.cacch.integration.controller.fdd;

import com.cacch.integration.common.enums.fdd.FddAuthStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.fdd.FddEnterpriseAuthDO;
import com.cacch.integration.integration.fdd.client.dto.FddCallbackRequest;
import com.cacch.integration.manager.fdd.api.IFddAuthManager;
import com.cacch.integration.service.fdd.api.IFddEnterpriseAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 法大大企业认证回调回放（仅 test 环境且开启开关，不影响正式 {@code /callback}）
 *
 * <p>用于在无新企业可发起认证时，基于历史 PENDING/终态记录验证回调处理链路。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Profile("test")
@ConditionalOnProperty(prefix = "fdd", name = "callback-test-enabled", havingValue = "true")
@RestController
@RequestMapping("/api/v1/fdd/callback/test")
@RequiredArgsConstructor
public class FddCallbackTestController {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final String LOG_BIZ = "FddCallbackTest";

    private final IFddAuthManager fddAuthManager;
    private final IFddEnterpriseAuthService fddEnterpriseAuthService;

    /**
     * 回放企业认证回调（最终仍调用正式 {@link IFddAuthManager#handleEnterpriseCallback}）
     *
     * @param body         回调原始 JSON（与法大大一致，如 notifyType/companyId/status/transactionNo）
     * @param forceReplay  历史记录已是 SUCCESS/FAILED 时是否先重置为 PENDING 再回放，默认 true
     * @return 回放前后状态摘要
     */
    @PostMapping("/enterprise")
    public Result<Map<String, Object>> replayEnterpriseCallback(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(defaultValue = "true") boolean forceReplay) {
        if (body == null || body.isEmpty()) {
            log.info("【{}】回放终止, reason=请求体为空", LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "回调请求体不能为空");
        }
        FddCallbackRequest request = OBJECT_MAPPER.convertValue(body, FddCallbackRequest.class);
        if (!StringUtils.hasText(request.getTransactionNo())) {
            log.info("【{}】回放终止, reason=transactionNo 为空", LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "transactionNo 不能为空");
        }

        String transactionNo = request.getTransactionNo().trim();
        FddEnterpriseAuthDO before = fddEnterpriseAuthService.findByTransactionNo(transactionNo);
        if (before == null) {
            log.info("【{}】回放终止, reason=未找到历史记录, transactionNo={}", LOG_BIZ, transactionNo);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "未找到对应企业认证历史记录, transactionNo=" + transactionNo);
        }

        boolean resetApplied = false;
        String statusBefore = before.getAuthStatus();
        if (forceReplay && isTerminal(statusBefore)) {
            before = fddEnterpriseAuthService.resetToPendingForCallbackTest(before.getId());
            resetApplied = true;
            log.info("【{}】历史终态已重置为 PENDING 以便回放, id={}, transactionNo={}, previousStatus={}",
                    LOG_BIZ, before.getId(), transactionNo, statusBefore);
        }

        // 与正式回调入口共用同一处理逻辑，保证验证结果一致
        fddAuthManager.handleEnterpriseCallback(request, body);

        FddEnterpriseAuthDO after = fddEnterpriseAuthService.findByTransactionNo(transactionNo);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("transactionNo", transactionNo);
        summary.put("companyId", request.getCompanyId());
        summary.put("callbackStatus", request.getStatus());
        summary.put("forceReplay", forceReplay);
        summary.put("resetApplied", resetApplied);
        summary.put("recordId", after != null ? after.getId() : before.getId());
        summary.put("authStatusBefore", statusBefore);
        summary.put("authStatusAfter", after != null ? after.getAuthStatus() : null);
        summary.put("fddCompanyId", after != null ? after.getFddCompanyId() : null);
        summary.put("message", "已走正式企业回调处理逻辑");
        log.info("【{}】企业回调回放完成, transactionNo={}, before={}, after={}, resetApplied={}",
                LOG_BIZ, transactionNo, statusBefore,
                after != null ? after.getAuthStatus() : null, resetApplied);
        return Result.success(summary);
    }

    private static boolean isTerminal(String authStatus) {
        return FddAuthStatusEnum.SUCCESS.getCode().equals(authStatus)
                || FddAuthStatusEnum.FAILED.getCode().equals(authStatus);
    }
}
