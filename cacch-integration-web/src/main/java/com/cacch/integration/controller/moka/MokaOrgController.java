package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.moka.MokaOrgConverter;
import com.cacch.integration.dto.moka.request.DeleteMokaDepartmentsRequest;
import com.cacch.integration.dto.moka.request.MokaOrgSyncRequest;
import com.cacch.integration.dto.moka.vo.MokaDeptSyncFromIhrResultVO;
import com.cacch.integration.dto.moka.vo.MokaDeptSyncResultVO;
import com.cacch.integration.dto.moka.vo.MokaDeptVO;
import com.cacch.integration.dto.moka.vo.MokaManualDeleteResultVO;
import com.cacch.integration.integration.moka.client.dto.MokaDeptListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.dto.moka.vo.MokaDeptPushResultVO;
import com.cacch.integration.manager.moka.api.IMokaDepartmentPushManager;
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
    private final IMokaDepartmentPushManager mokaDeptPushManager;

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
        log.info("收到 Moka 组织架构全量同步请求, departmentCount={}",
                request.getDepartments() == null ? 0 : request.getDepartments().size());
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
     * 从本地 IHR 部门快照同步到 Moka 表（手动触发）
     *
     * <p>执行流程：
     * <ol>
     *     <li>从本地 {@code t_integration_ihr_department} 表读取 {@code department_status='ENABLE'} 的部门快照</li>
     *     <li>字段映射：iHR 字段 → t_integration_moka_department 主表 + t_integration_moka_department_localized 子表</li>
     *     <li>批量 upsert 落库（主表按 department_code upsert，子表固定 locale=zh_CN；
     *     iHR 重新启用已删部门时复活：状态重置 0、moka_delete_time 置 NULL）</li>
     *     <li>差集标记待删除：本地 Moka 表中不在 IHR ENABLE 集合、状态非 3/4 的部门置 3（待删除），
     *     由推送接口（POST /push-to-moka）的前置删除步骤调 Moka 删除接口处理</li>
     * </ol>
     *
     * <p>本接口不再回源调用 IHR 开放平台，IHR 数据刷新由独立的
     * {@code POST /api/v1/ihr/departments/sync} 接口负责。建议先刷新 IHR 快照再调用本接口。</p>
     *
     * <p>请求体为空（{} 或不传）即可触发；同步过程为同步阻塞，建议在低峰期执行。</p>
     *
     * @return 同步执行结果，含总读取数 / 主表成功数 / 子表成功数 / 跳过数 / 标记待删除数
     */
    @PostMapping("/sync-from-ihr")
    public Result<MokaDeptSyncFromIhrResultVO> syncFromIhr() {
        log.info("【MokaDeptSyncFromIhr】开始执行 IHR 本地快照 → Moka 部门同步");
        IMokaDepartmentSyncManager.MokaDeptSyncResult result = mokaDeptSyncManager.syncFromIhr();
        log.info("【MokaDeptSyncFromIhr】同步完成, totalFetched={}, deptUpserted={}, localizedUpserted={}, deptSkipped={}, markedDeleteCount={}",
                result.totalFetched(), result.deptUpserted(), result.localizedUpserted(), result.deptSkipped(), result.markedDeleteCount());
        return Result.success(MokaDeptSyncFromIhrResultVO.from(result));
    }

    /**
     * 将本地 PG 表全量推送到 Moka 开放平台（手动触发，前置处理待删除部门）
     *
     * <p>执行流程：
     * <ol>
     *     <li>前置删除步骤：查询 {@code moka_sync_status=3}（待删除）的部门，经孤儿校验与
     *     自底向上排序后调 Moka DELETE /api-platform/v1/departments 标记删除；
     *     成功置 4（已删除，记录 moka_delete_time），失败保留 3 待下轮重试。
     *     {@code moka.delete-email}（Moka 注册邮箱）未配置时本步骤跳过</li>
     *     <li>查询本地 PG 表全量记录（排除待删除 3 / 已删除 4）与子表多语言数据</li>
     *     <li>DO → Moka API DTO 字段映射（含多语言填充）</li>
     *     <li>调用 Moka PUT /api-platform/v2/departments 全量同步</li>
     *     <li>根据 API 返回结果更新本地 moka_sync_status：成功→1(SYNCED，记录 moka_sync_time)、失败→2(SYNC_FAILED)</li>
     * </ol>
     *
     * <p>全量推送语义：Moka API 以 departmentCode 为主键，本次传入的 departments 即 Moka 侧完整最新列表。
     * 因此推送范围必须是本地 PG 全量（排除待删/已删），不能只推 PENDING/FAILED 的子集。</p>
     *
     * @return 推送执行结果，含 Moka 侧 new/update/delete 计数 + 本地状态更新条数 + 删除步骤执行结果
     */
    @PostMapping("/push-to-moka")
    public Result<MokaDeptPushResultVO> pushToMoka() {
        log.info("【MokaDeptPushToMoka】开始执行本地 PG → Moka 开放平台部门全量推送（含待删除部门处理）");
        IMokaDepartmentPushManager.MokaDeptPushResult result = mokaDeptPushManager.pushToMoka();
        log.info("【MokaDeptPushToMoka】推送完成, totalPushed={}, mokaApiSuccess={}, newOnMoka={}, updatedOnMoka={}, deletedOnMoka={}, syncedCount={}, syncFailedCount={}, dbUpdateFailed={}, deletePendingCount={}, deleteSkippedCount={}, deleteSuccess={}, deletedCount={}",
                result.totalPushed(), result.mokaApiSuccess(), result.newOnMoka(), result.updatedOnMoka(), result.deletedOnMoka(), result.syncedCount(), result.syncFailedCount(), result.dbUpdateFailedCount(), result.deletePendingCount(), result.deleteSkippedCount(), result.deleteSuccess(), result.deletedCount());
        return Result.success(MokaDeptPushResultVO.from(result));
    }

    /**
     * 手动触发部门删除 —— 标记指定部门为待删除并立即调 Moka 删除接口
     *
     * <p>用于对本地已有部门数据（状态 0-未同步 / 1-已同步 / 2-同步失败）手动执行删除，
     * 弥补仅靠 iHR 快照差集产生待删除记录的不足。</p>
     *
     * <p>入口模式：
     * <ul>
     *     <li>departmentCodes 非空（手动模式）：按传入编码列表执行删除</li>
     *     <li>departmentCodes 为空（自动模式）：自动查询本地 iHR 快照中
     *     {@code department_status='DISABLE'}（停用）的部门编码全集执行删除</li>
     * </ul>
     *
     * <p>执行流程：
     * <ol>
     *     <li>部门编码校验（逐条，单条失败不阻断）：本地不存在的跳过并计数；
     *     状态已是 4（已删除）的跳过标记；状态 0/1/2 的置 3（待删除）；已是 3 的无需重复标记</li>
     *     <li>立即执行删除编排（与 push-to-moka 前置删除步骤同一套逻辑，处理全部状态 3 部门，
     *     含此前差集标记的历史待删部门）：孤儿校验（存在未删除子部门的跳过）→
     *     树深度自底向上排序 → 调 Moka DELETE /api-platform/v1/departments →
     *     成功置 4（记录 moka_delete_time），失败保留 3 待下轮 push-to-moka 重试</li>
     * </ol>
     *
     * <p>注意：{@code moka.delete-email}（Moka 注册邮箱）未配置时仅完成标记，删除步骤跳过
     * （deleteSuccess 返回 null），后续可通过 push-to-moka 补偿执行。</p>
     *
     * @param request 手动删除请求体；departmentCodes 可选（为空时自动按 iHR 停用部门执行，
     *                非空时列表项不可为空白）
     * @return 手动删除执行结果，含入口模式标识（autoResolved）+ 编码校验计数 + 删除编排执行结果
     */
    @PostMapping("/delete-departments")
    public Result<MokaManualDeleteResultVO> deleteDepartments(@Valid @RequestBody DeleteMokaDepartmentsRequest request) {
        log.info("【MokaManualDelete】收到 Moka 部门手动删除请求, departmentCodeCount={}",
                request.getDepartmentCodes() == null ? 0 : request.getDepartmentCodes().size());
        IMokaDepartmentPushManager.MokaManualDeleteResult result =
                mokaDeptPushManager.deleteDepartments(request.getDepartmentCodes());
        log.info("【MokaManualDelete】手动删除完成, autoResolved={}, requestCount={}, notFoundCount={}, alreadyDeletedCount={}, markedCount={}, deletePendingCount={}, deleteSkippedCount={}, deleteSuccess={}, deletedCount={}",
                result.autoResolved(), result.requestCount(), result.notFoundCount(), result.alreadyDeletedCount(), result.markedCount(),
                result.deletePendingCount(), result.deleteSkippedCount(), result.deleteSuccess(), result.deletedCount());
        return Result.success(MokaManualDeleteResultVO.from(result));
    }
}
