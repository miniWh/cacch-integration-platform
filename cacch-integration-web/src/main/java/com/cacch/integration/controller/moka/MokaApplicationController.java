package com.cacch.integration.controller.moka;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.convert.moka.MokaApplicationConverter;
import com.cacch.integration.dto.moka.request.MokaApplicationHiredRequest;
import com.cacch.integration.dto.moka.request.MokaApplicationResignRequest;
import com.cacch.integration.dto.moka.vo.MokaApplicationHiredResultVO;
import com.cacch.integration.dto.moka.vo.MokaApplicationResignResultVO;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationHiredResponse;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignResponse;
import com.cacch.integration.service.moka.api.IMokaApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moka 入离职 REST 接口（统一入口）
 *
 * <p>当前暴露接口：
 * <ul>
 *     <li>PUT /{applicationId}/resign —— 标记候选人已离职（透传 Moka API）</li>
 *     <li>PUT /{applicationId}/hired —— 标记候选人已入职（透传 Moka API）</li>
 * </ul></p>
 *
 * <p>鉴权：Moka API Key 由配置文件 {@code moka.api-key}（经环境变量 {@code MOKA_API_KEY} 注入）注入，
 * 调用方无需传递密钥。</p>
 *
 * <p>注意：标记离职为 Moka 侧写操作，调用成功后申请将归档为已离职且不可自动回滚，
 * 请务必使用测试数据验证。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/moka/applications")
@RequiredArgsConstructor
public class MokaApplicationController {

    private final IMokaApplicationService mokaApplicationService;
    private final MokaApplicationConverter mokaApplicationConverter;

    /**
     * 标记候选人已离职 —— 透传 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/resign}
     *
     * <p>校验规则：
     * <ol>
     *     <li>applicationId 必须为正整数</li>
     *     <li>detail（离职原因）必填</li>
     *     <li>isResignWriteJob=1 时 resignJobName 必填（条件依赖，手动校验）</li>
     * </ol></p>
     *
     * @param applicationId 候选人申请 id（路径参数，正整数）
     * @param request       离职请求体，detail 必填
     * @return 离职操作结果 VO（success + code + errorMessage）
     * @throws BizException 参数非法（40002/40001）或 Moka 调用失败（50001）时抛出，
     *                      由全局异常处理器统一转换为 Result 返回
     */
    @PutMapping("/{applicationId}/resign")
    public Result<MokaApplicationResignResultVO> resignApplication(
            @PathVariable("applicationId")
            @Positive(message = "applicationId 必须为正整数") Integer applicationId,
            @Valid @RequestBody MokaApplicationResignRequest request) {
        log.info("【MokaApplicationController】收到标记候选人已离职请求, applicationId={}", applicationId);

        // —— 条件依赖校验：isResignWriteJob=1 时 resignJobName 必填 ——
        boolean writeJobEnabled = request.getIsResignWriteJob() != null && request.getIsResignWriteJob() == 1;
        boolean jobNameBlank = request.getResignJobName() == null || request.getResignJobName().isBlank();
        if (writeJobEnabled && jobNameBlank) {
            log.info("【MokaApplicationController】参数校验终止, reason=isResignWriteJob=1 但 resignJobName 为空, applicationId={}",
                    applicationId);
            throw new BizException(ResultCode.PARAM_MISSING, "isResignWriteJob=1 时离职职位 resignJobName 必填");
        }

        MokaApplicationResignResponse response =
                mokaApplicationService.resignApplication(applicationId,
                        mokaApplicationConverter.toUpstreamResignRequest(request));
        return Result.success(mokaApplicationConverter.toResignResultVO(response));
    }

    /**
     * 标记候选人已入职 —— 透传 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/hired}
     *
     * <p>校验规则：
     * <ol>
     *     <li>applicationId 必须为正整数</li>
     *     <li>hiredAt（实际入职时间）必填</li>
     *     <li>probation（试用期）必填，取值 0-6</li>
     *     <li>hcId 可选，若传须为正整数；isUseOfferHcId 可选，取值 0/1</li>
     * </ol></p>
     *
     * @param applicationId 候选人申请 id（路径参数，正整数）
     * @param request       入职请求体，hiredAt / probation 必填
     * @return 入职操作结果 VO（success + errorMessage）
     * @throws BizException 参数非法（40002/40001）或 Moka 调用失败（50001）时抛出，
     *                      由全局异常处理器统一转换为 Result 返回
     */
    @PutMapping("/{applicationId}/hired")
    public Result<MokaApplicationHiredResultVO> hireApplication(
            @PathVariable("applicationId")
            @Positive(message = "applicationId 必须为正整数") Integer applicationId,
            @Valid @RequestBody MokaApplicationHiredRequest request) {
        log.info("【MokaApplicationController】收到标记候选人已入职请求, applicationId={}", applicationId);

        MokaApplicationHiredResponse response =
                mokaApplicationService.hireApplication(applicationId,
                        mokaApplicationConverter.toUpstreamHiredRequest(request));
        return Result.success(mokaApplicationConverter.toHiredResultVO(response));
    }
}
