package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.moka.MokaJobConverter;
import com.cacch.integration.dto.moka.request.MokaJobListRequestVO;
import com.cacch.integration.dto.moka.vo.MokaJobListVO;
import com.cacch.integration.integration.moka.client.dto.MokaJobListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaJobListResponse;
import com.cacch.integration.service.moka.api.IMokaJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moka 职位 REST 接口
 *
 * <p>当前暴露接口：
 * <ul>
 *     <li>{@link #getJobs(MokaJobListRequestVO)} — 职位查询（透传 Moka API）</li>
 * </ul></p>
 *
 * <p>鉴权：Moka API Key 由配置文件 {@code moka.api-key}（经环境变量 {@code MOKA_API_KEY} 注入）注入，
 * 调用方无需传递密钥。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/moka/jobs")
@RequiredArgsConstructor
public class MokaJobController {

    private final IMokaJobService mokaJobService;
    private final MokaJobConverter mokaJobConverter;

    /**
     * 查询职位信息 —— 透传 Moka API {@code POST /api-platform/v1/jobs/getJobs}
     *
     * <p>按招聘模式（必填）及地点、职能、官网、职位性质、状态、部门、门店、
     * 更新时间、自定义字段等可选条件查询职位，出参全量透传 Moka 返回的职位字段。</p>
     *
     * @param requestVO 查询条件；hireMode 必填（social=社招 / campus=校招），其余字段可选
     * @return 职位列表（含 total 职位总数与 jobs 职位明细，全量字段透传）
     */
    @PostMapping("/list")
    public Result<MokaJobListVO> getJobs(@Valid @RequestBody MokaJobListRequestVO requestVO) {
        log.info("【MokaJobs】开始查询职位, hireMode={}, status={}, limit={}, offset={}",
                requestVO.getHireMode(), requestVO.getStatus(),
                requestVO.getLimit(), requestVO.getOffset());

        MokaJobListRequest clientRequest = mokaJobConverter.toClientRequest(requestVO);
        MokaJobListResponse response = mokaJobService.getJobs(clientRequest);
        MokaJobListVO resultVO = mokaJobConverter.toListVO(response.getData());

        log.info("【MokaJobs】查询完成, total={}, jobSize={}",
                resultVO == null || resultVO.getTotal() == null ? 0 : resultVO.getTotal(),
                resultVO == null || resultVO.getJobs() == null ? 0 : resultVO.getJobs().size());
        return Result.success(resultVO);
    }
}
