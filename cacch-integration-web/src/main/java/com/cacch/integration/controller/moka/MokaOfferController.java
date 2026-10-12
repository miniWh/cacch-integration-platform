package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.moka.MokaOfferConverter;
import com.cacch.integration.dto.moka.request.MokaOfferExtendInfoRequestVO;
import com.cacch.integration.dto.moka.vo.MokaOfferExtendInfoVO;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoResponse;
import com.cacch.integration.service.moka.api.IMokaOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moka offer 扩展信息 REST 接口
 *
 * <p>当前暴露接口：
 * <ul>
 *     <li>POST /api/v1/moka/offers/extend-infos —— 按申请 ID 列表查询 offer 扩展信息（透传 Moka API）</li>
 * </ul>
 *
 * <p>鉴权：Moka API Key 由配置文件 {@code moka.api-key}（经环境变量 {@code MOKA_API_KEY} 注入），
 * 租户 ID（orgId）由 {@code moka.org-id}（环境变量 {@code MOKA_ORG_ID}，兜底 "cac"）注入，
 * 调用方无需传递密钥或租户 ID。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/moka/offers")
@RequiredArgsConstructor
public class MokaOfferController {

    private final IMokaOfferService mokaOfferService;
    private final MokaOfferConverter mokaOfferConverter;

    /**
     * 获取 offer 扩展信息 —— 透传 Moka API
     * {@code POST /api-platform/v3/getOfferExtendInfos}
     *
     * <p>校验规则：
     * <ol>
     *     <li>applicationIds 必填，非空集合</li>
     *     <li>单批次最多 100 个申请 ID</li>
     *     <li>offerAttachment 可选，不传时 Moka 默认 false</li>
     * </ol></p>
     *
     * @param request 请求体，含 applicationIds 与可选 offerAttachment
     * @return offer 扩展信息结果 VO（success + code + msg + data 列表）
     * @throws com.cacch.integration.common.exception.BizException 参数非法（40001/40002）
     *                                                             或 Moka 调用失败（50001/50003）时抛出，
     *                                                             由全局异常处理器统一转换为 Result 返回
     */
    @PostMapping("/extend-infos")
    public Result<MokaOfferExtendInfoVO> getOfferExtendInfo(@Valid @RequestBody MokaOfferExtendInfoRequestVO request) {
        log.info("【MokaOfferController】收到获取offer扩展信息请求, size={}, offerAttachment={}",
                request.getApplicationIds() != null ? request.getApplicationIds().size() : 0,
                request.getOfferAttachment());

        MokaOfferExtendInfoResponse response = mokaOfferService.getOfferExtendInfo(
                request.getApplicationIds(), request.getOfferAttachment());
        return Result.success(mokaOfferConverter.toVO(response));
    }
}
