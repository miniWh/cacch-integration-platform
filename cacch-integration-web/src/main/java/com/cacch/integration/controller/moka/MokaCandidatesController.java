package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse;
import com.cacch.integration.service.moka.api.IMokaCandidatesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moka 候选人 REST 接口（统一入口）
 *
 * <p>当前暴露接口：
 * <ul>
 *     <li>{@link #listApplications(MokaApplicationListRequest)} — 根据条件查询申请信息（透传 Moka API）</li>
 * </ul></p>
 *
 * <p>鉴权：Moka API Key 由配置文件 {@code moka.api-key}（经环境变量 {@code MOKA_API_KEY} 注入）注入，
 * 调用方无需传递密钥。</p>
 *
 * <p>历史：listApplications 原在 MokaPersonController，2026-10 迁入本 Controller，
 * URL 从 {@code /api/v1/moka/persons/applications/list} 调整为
 * {@code /api/v1/moka/candidates/applications/list}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/moka/candidates")
@RequiredArgsConstructor
public class MokaCandidatesController {

    private final IMokaCandidatesService mokaCandidatesService;

    /**
     * 根据条件查询申请信息（分页）—— 透传 Moka API
     * {@code POST /api-platform/v3/applications/list_by_condition}
     *
     * <p>支持按申请 id、候选人 id、职位 id、流程、阶段、招聘模式、归档状态、时间范围等条件查询。
     * 分页通过 {@code limit}（默认 20，最大 20）和 {@code next}（游标）实现。</p>
     *
     * <p>请求体所有字段均可选，传空对象 {@code {}} 表示无条件返回全部。</p>
     *
     * @param request 查询条件；所有字段均可选，null 时自动兜底空对象
     * @return 申请分页响应（直接透传 Moka 返回结构，含 code=200 成功码）
     */
    @PostMapping("/applications/list")
    public Result<MokaApplicationListResponse> listApplications(
            @RequestBody(required = false) MokaApplicationListRequest request) {
        log.info("【MokaCandidates】开始查询申请信息, jobId={}, hireMode={}",
                request == null ? null : request.getJobId(),
                request == null ? null : request.getHireMode());
        MokaApplicationListResponse response = mokaCandidatesService.listApplications(request);
        int listSize = response.getData() == null || response.getData().getApplicationList() == null
                ? 0 : response.getData().getApplicationList().size();
        log.info("【MokaCandidates】查询完成, listSize={}, hasNext={}",
                listSize, response.getData() != null && response.getData().getNext() != null);
        return Result.success(response);
    }
}
