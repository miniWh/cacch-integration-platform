package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.moka.MokaOrgConverter;
import com.cacch.integration.dto.moka.request.MokaOrgSyncRequest;
import com.cacch.integration.dto.moka.vo.MokaDeptSyncResultVO;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.service.moka.api.IMokaOrgService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moka 组织架构 REST 接口（测试用）
 *
 * <p>对外暴露「组织架构全量同步」接口，调用方传入部门列表，本服务透传至 Moka。
 * 鉴权：Moka API Key 由配置文件 {@code moka.api-key}（经环境变量 {@code MOKA_API_KEY} 注入）注入，
 * 调用方无需传递密钥。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/moka/departments")
@RequiredArgsConstructor
public class MokaOrgController {

    private final IMokaOrgService mokaOrgService;
    private final MokaOrgConverter mokaOrgConverter;

    /**
     * 组织架构全量同步 —— 透传 Moka API {@code PUT /api-platform/v2/departments}
     *
     * <p>同步以 departmentCode 为主键，Moka 侧自动执行新增 / 更新 / 标记删除。
     * 本次传入中未包含的、但系统内已存在的部门会被标记为删除（需手动在 Moka 后台合并删除）。</p>
     *
     * @param request 部门同步请求体，departments 列表必填（至少一条）
     * @return 同步结果 VO，含 new / update / delete 计数
     */
    @PutMapping("/full-sync")
    public Result<MokaDeptSyncResultVO> syncDepartmentsFull(@Valid @RequestBody MokaOrgSyncRequest request) {
        log.info("收到 Moka 组织架构全量同步请求, departmentCount={}, operatorEmail={}",
                request.getDepartments() == null ? 0 : request.getDepartments().size(),
                request.getOperatorEmail());
        MokaDeptSyncRequest upstreamRequest = mokaOrgConverter.toUpstreamRequest(request);
        return Result.success(mokaOrgConverter.toSyncResultVO(mokaOrgService.syncDepartmentsFull(upstreamRequest)));
    }
}
