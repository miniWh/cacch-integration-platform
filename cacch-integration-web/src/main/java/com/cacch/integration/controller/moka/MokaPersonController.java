package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.moka.vo.MokaPersonPushResultVO;
import com.cacch.integration.dto.moka.vo.MokaPersonSyncResultVO;
import com.cacch.integration.manager.moka.api.IMokaPersonPushManager;
import com.cacch.integration.manager.moka.api.IMokaPersonSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moka 人员同步 REST 接口
 *
 * <p>当前暴露接口：
 * <ul>
 *     <li>{@link #syncFromIhr()} —— 接口 1：persondetail 外部表 → Moka 人员中间表</li>
 *     <li>{@link #pushToMoka()} —— 接口 2：Moka 人员中间表 → Moka 开放平台推送</li>
 * </ul>
 *
 * <p>鉴权：外部 persondetail / ihr_department 均为只读查询，
 * 中间表 upsert 由 Service 层显式事务管理；接口 2 的 HTTP 调用不包事务，
 * DB 状态回写由 Service 层显式声明事务。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/moka/persons")
@RequiredArgsConstructor
public class MokaPersonController {

    private final IMokaPersonSyncManager mokaPersonSyncManager;
    private final IMokaPersonPushManager mokaPersonPushManager;

    /**
     * 从 persondetail 外部表同步全量员工到 Moka 人员中间表
     *
     * <p>执行流程：
     * <ol>
     *     <li>读取 persondetail 全量员工（@Select 手写 SQL）</li>
     *     <li>批量收集 departmentId → 查询 ihr_department 得到 department_code</li>
     *     <li>逐条字段映射 + 校验 + 构造 MokaPersonDO</li>
     *     <li>superiorsInfo 解析提取直属邮箱（防御性，结构未知时置 null）</li>
     *     <li>roleId 阶段一不赋值，Service 层兜底 DEFAULT 223379</li>
     *     <li>批量 upsert 到 t_integration_moka_person</li>
     * </ol>
     *
     * <p>请求体为空（{} 或不传）即可触发；同步过程为同步阻塞，
     * 人员量预估 ≤ 1000，正常预计耗时 ≤ 30s。</p>
     *
     * @return 同步执行结果，含总数 / upsert 成功数 / 跳过数
     */
    @PostMapping("/sync-from-ihr")
    public Result<MokaPersonSyncResultVO> syncFromIhr() {
        log.info("【MokaPersonSyncFromIhr】开始执行 persondetail → Moka 人员中间表同步");
        IMokaPersonSyncManager.MokaPersonSyncResult result = mokaPersonSyncManager.syncFromIhr();
        log.info("【MokaPersonSyncFromIhr】同步完成, totalFetched={}, personUpserted={}, deptCodeSkipped={}, invalidSkipped={}",
                result.totalFetched(), result.personUpserted(),
                result.deptCodeSkipped(), result.invalidSkipped());
        return Result.success(MokaPersonSyncResultVO.from(result));
    }

    /**
     * 推送 Moka 人员中间表 → Moka 开放平台
     *
     * <p>执行流程：
     * <ol>
     *     <li>查询 moka_sync_status IN (0, 2) 的待推送记录（PENDING + SYNC_FAILED）</li>
     *     <li>按每批 ≤ 100 条切分</li>
     *     <li>每批字段映射 → MokaUserSyncRequest → 调用 Moka syncInfo API</li>
     *     <li>Moka API 整批成功 → 回写 sync_status=1（SYNCED）；失败 → 回写 sync_status=2（SYNC_FAILED）</li>
     * </ol>
     *
     * <p>请求体为空（{} 或不传）即可触发；推送过程为同步阻塞，
     * 推送条数预估 ≤ 1000（4 批），正常预计耗时 ≤ 60s。
     * 幂等性：Moka syncInfo API 以手机号为唯一键做 upsert，可重复推送。</p>
     *
     * @return 推送结果，含总读取数 / 批次数 / API 成功标记 / 同步成功数 / 同步失败数 / DB 更新失败数
     */
    @PostMapping("/push-to-moka")
    public Result<MokaPersonPushResultVO> pushToMoka() {
        log.info("【MokaPersonPushToMoka】开始执行 Moka 人员中间表 → Moka 开放平台推送");
        IMokaPersonPushManager.MokaPersonPushResult result = mokaPersonPushManager.pushToMoka();
        log.info("【MokaPersonPushToMoka】推送完成, totalRead={}, batchCount={}, mokaApiSuccess={}, syncedCount={}, syncFailedCount={}, dbUpdateFailedCount={}",
                result.totalRead(), result.batchCount(), result.mokaApiSuccess(),
                result.syncedCount(), result.syncFailedCount(), result.dbUpdateFailedCount());
        return Result.success(MokaPersonPushResultVO.from(result));
    }
}
