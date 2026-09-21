package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.moka.vo.MokaPersonSyncResultVO;
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
 * <p>当前暴露接口：从 persondetail 外部表同步员工到 Moka 人员中间表。
 * 接口 2（中间表 → Moka 开放平台推送）后续扩展。</p>
 *
 * <p>鉴权：外部 persondetail / ihr_department 均为只读查询，
 * 中间表 upsert 由 Service 层显式事务管理。</p>
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
}
