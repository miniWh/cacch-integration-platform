package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.moka.MokaOrgConverter;
import com.cacch.integration.dto.moka.request.MokaOrgSyncRequest;
import com.cacch.integration.dto.moka.vo.MokaDeptSyncFromIhrResultVO;
import com.cacch.integration.dto.moka.vo.MokaDeptSyncResultVO;
import com.cacch.integration.dto.moka.vo.MokaDeptVO;
import com.cacch.integration.integration.moka.client.dto.MokaDeptListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.manager.moka.api.IMokaDepartmentSyncManager;
import com.cacch.integration.service.moka.api.IMokaOrgService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Moka 组织架构 REST 接口（测试用）
 *
 * <p>对外暴露三类接口：
 * <ul>
 *     <li>Moka 开放平台 API 透传（PUT full-sync / GET departments）—— 调用方 → 本服务 → Moka</li>
 *     <li>IHR → Moka 本地表同步（POST sync-from-ihr）—— 本服务 → IHR 拉全量 → 落 PG 表</li>
 * </ul>
 *
 * <p>鉴权：Moka API Key 由配置文件 {@code moka.api-key}（经环境变量 {@code MOKA_API_KEY} 注入）注入，
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
    private final IMokaDepartmentSyncManager mokaDeptSyncManager;

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

    /**
     * 获取全量组织架构 —— 透传 Moka API {@code GET /api-platform/v1/departments}
     *
     * <p>返回 Moka 侧全量部门列表。支持可选 {@code updateTimeStart} 增量查询参数
     * （格式 {@code yyyy-MM-dd HH:mm:ss}），为空时返回全量数据。</p>
     *
     * @param updateTimeStart 增量查询起始时间（可选，格式 yyyy-MM-dd HH:mm:ss）
     * @return Moka 部门列表 VO
     */
    @GetMapping
    public Result<List<MokaDeptVO>> getDepartments(
            @RequestParam(value = "updateTimeStart", required = false) String updateTimeStart) {
        log.info("收到 Moka 获取全量组织架构请求, updateTimeStart={}", updateTimeStart);
        MokaDeptListResponse response = mokaOrgService.getDepartments(updateTimeStart);
        return Result.success(mokaOrgConverter.toDeptVOList(response));
    }

    /**
     * 从 IHR 全量同步部门到本地 Moka 表（手动触发）
     *
     * <p>执行流程：
     * <ol>
     *     <li>调用 IHR 部门查询接口，循环翻页拉取全量数据</li>
     *     <li>字段映射：iHR 字段 → t_integration_moka_department 主表 + t_integration_moka_department_localized 子表</li>
     *     <li>批量 upsert 落库（主表按 department_code upsert，子表固定 locale=zh_CN）</li>
     * </ol>
     *
     * <p>请求体为空（{} 或不传）即可触发；同步过程为同步阻塞，建议在低峰期执行。</p>
     *
     * @return 同步执行结果，含总拉取数 / 主表成功数 / 子表成功数 / 跳过数
     */
    @PostMapping("/sync-from-ihr")
    public Result<MokaDeptSyncFromIhrResultVO> syncFromIhr() {
        log.info("【MokaDeptSyncFromIhr】开始执行 IHR → Moka 全量部门同步");
        IMokaDepartmentSyncManager.MokaDeptSyncResult result = mokaDeptSyncManager.syncFromIhr();
        log.info("【MokaDeptSyncFromIhr】同步完成, totalFetched={}, deptUpserted={}, localizedUpserted={}, deptSkipped={}",
                result.totalFetched(), result.deptUpserted(), result.localizedUpserted(), result.deptSkipped());
        return Result.success(MokaDeptSyncFromIhrResultVO.from(result));
    }
}
