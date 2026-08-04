package com.cacch.integration.controller.fdd;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.fdd.client.dto.FddCallbackRequest;
import com.cacch.integration.manager.fdd.api.IFddAuthManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * 法大大认证结果回调接收（匿名接口，不验签）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/fdd")
@RequiredArgsConstructor
public class FddCallbackController {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final IFddAuthManager fddAuthManager;

    /**
     * 接收法大大认证结果回调
     *
     * @param body 原始回调 JSON
     * @return 法大大约定成功字符串 success
     */
    @PostMapping("/callback")
    public String callback(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            log.info("【FddCallback】回调终止, reason=请求体为空");
            throw new BizException(ResultCode.PARAM_MISSING, "回调请求体不能为空");
        }
        FddCallbackRequest request = OBJECT_MAPPER.convertValue(body, FddCallbackRequest.class);
        log.info("【FddCallback】收到回调, notifyType={}, transactionNo={}, status={}, tpOrgId={}",
                request.getNotifyType(), request.getTransactionNo(), request.getStatus(), request.getTpOrgId());

        String notifyType = request.getNotifyType();
        if (!StringUtils.hasText(notifyType)) {
            log.info("【FddCallback】回调终止, reason=notifyType 为空");
            throw new BizException(ResultCode.PARAM_MISSING, "notifyType 不能为空");
        }
        if (FddConstants.NOTIFY_TYPE_ENTERPRISE.equalsIgnoreCase(notifyType.trim())) {
            fddAuthManager.handleEnterpriseCallback(request, body);
            return "success";
        }
        if (FddConstants.NOTIFY_TYPE_PERSON.equalsIgnoreCase(notifyType.trim())) {
            log.info("【FddCallback】个人认证回调暂未实现, transactionNo={}", request.getTransactionNo());
            throw new BizException(ResultCode.PARAM_INVALID, "个人认证回调暂未开放");
        }
        log.info("【FddCallback】回调终止, reason=未知 notifyType={}", notifyType);
        throw new BizException(ResultCode.PARAM_INVALID, "未知 notifyType: " + notifyType);
    }
}
