package com.cacch.integration.controller.fdd;

import com.cacch.integration.common.dto.fdd.FddAuthQueryResult;
import com.cacch.integration.common.dto.fdd.FddEnterpriseAuthQueryCommand;
import com.cacch.integration.common.enums.fdd.FddAuthTypeEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.convert.fdd.FddAuthConverter;
import com.cacch.integration.dto.fdd.request.FddAuthQueryRequest;
import com.cacch.integration.dto.fdd.vo.FddAuthQueryVO;
import com.cacch.integration.manager.fdd.api.IFddAuthManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 法大大实名认证对外接口（企业认证）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/fdd/auth")
@RequiredArgsConstructor
public class FddAuthController {

    private final IFddAuthManager fddAuthManager;
    private final FddAuthConverter fddAuthConverter;

    /**
     * 统一查询 / 自动发起认证
     *
     * @param request 查询请求（须含 internalCompanyName；企业认证须含 uscc）
     * @return 认证状态与可选 authUrl
     */
    @PostMapping("/query")
    public Result<FddAuthQueryVO> query(@Valid @RequestBody FddAuthQueryRequest request) {
        FddAuthTypeEnum authType = FddAuthTypeEnum.fromCode(request.getAuthType());
        if (authType == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "authType 仅允许 ENTERPRISE 或 PERSON");
        }
        if (authType == FddAuthTypeEnum.PERSON) {
            log.info("【FddAuth】个人认证暂未实现, internalCompany={}", request.getInternalCompanyName());
            throw new BizException(ResultCode.PARAM_INVALID, "个人认证能力暂未开放，请使用 ENTERPRISE");
        }
        FddAuthQueryResult result = fddAuthManager.queryOrAuthEnterprise(
                new FddEnterpriseAuthQueryCommand(
                        request.getInternalCompanyName(),
                        request.getEnterpriseName(),
                        request.getUscc(),
                        request.getAutoAuth(),
                        request.getSourceSystem(),
                        request.getSourceBizNo()
                ));
        return Result.success(fddAuthConverter.toVO(result));
    }

    /**
     * 按业务键查询企业认证最新状态（不自动发起）
     *
     * @param authType            认证类型，当前仅支持 ENTERPRISE
     * @param internalCompanyName 内部企业全称
     * @param uscc                统一社会信用代码
     * @return 认证状态
     */
    @GetMapping("/status")
    public Result<FddAuthQueryVO> status(@RequestParam String authType,
                                         @RequestParam String internalCompanyName,
                                         @RequestParam(required = false) String uscc) {
        FddAuthTypeEnum type = FddAuthTypeEnum.fromCode(authType);
        if (type == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "authType 仅允许 ENTERPRISE 或 PERSON");
        }
        if (type == FddAuthTypeEnum.PERSON) {
            throw new BizException(ResultCode.PARAM_INVALID, "个人认证能力暂未开放，请使用 ENTERPRISE");
        }
        if (!StringUtils.hasText(uscc)) {
            throw new BizException(ResultCode.PARAM_MISSING, "uscc 不能为空");
        }
        FddAuthQueryResult result = fddAuthManager.queryOrAuthEnterprise(
                new FddEnterpriseAuthQueryCommand(
                        internalCompanyName, null, uscc, Boolean.FALSE, null, null));
        return Result.success(fddAuthConverter.toVO(result));
    }
}
