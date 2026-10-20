package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.moka.vo.MokaPersonStatusSyncResultVO;
import com.cacch.integration.manager.moka.api.IMokaPersonStatusSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moka 候选人入离职状态同步 REST 接口
 *
 * <p>职责：将 t_integration_moka_entry_candidate_base（待入职候选人快照）中
 * 有记录的员工（INNER JOIN 天然过滤无候选人），按 contact_phone = moka_contact_phone
 * 关联 t_integration_moka_person，再按 employee_status 触发
 * Moka ATS 申请状态接口 hired / resign。</p>
 *
 * <p>事务与鉴权：Manager 编排层不包外层事务，Moka HTTP 调用不包事务，
 * 状态行回写由 Service 层显式声明事务；单条失败不阻断其余条目，可重复触发（幂等）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/moka/persons")
@RequiredArgsConstructor
public class MokaPersonStatusController {

    private final IMokaPersonStatusSyncManager mokaPersonStatusSyncManager;

    /**
     * 推送候选人入离职状态到 Moka
     *
     * <p>执行流程：
     * <ol>
     *     <li>INNER JOIN candidate_base 查询入职 / 离职待处理集合
     *     （contact_phone = moka_contact_phone，天然过滤无候选人记录）</li>
     *     <li>按工号（employee_no = persondetail.staffNo）关联 persondetail 取入职 / 离职日期</li>
     *     <li>在职：调 Moka hired 接口（hiredAt=contractBeginDate，probation=整月差值 0-6）</li>
     *     <li>离职：调 Moka resign 接口（time=leaveDate，detail=固定文案「iHR 同步-员工已离职」）</li>
     *     <li>逐条回写 t_integration_moka_person_status（upsert，幂等可重跑）</li>
     * </ol></p>
     *
     * <p>请求体为空（{} 或不传）即可触发；同步过程为逐条串行，人员量预估 ≤ 1000。
     * 单条失败不阻断其余条目。</p>
     *
     * @return 同步执行结果，含总读取数 / 入离职成功失败数 / 回写失败数
     */
    @PostMapping("/push-status-to-moka")
    public Result<MokaPersonStatusSyncResultVO> pushStatusToMoka() {
        log.info("【MokaCandidateStatus】开始执行候选人入离职状态 → Moka 申请状态同步");
        IMokaPersonStatusSyncManager.MokaPersonStatusSyncResult result = mokaPersonStatusSyncManager.pushStatusToMoka();
        log.info("【MokaCandidateStatus】同步完成, totalRead={}, hiredSuccess={}, hiredFailed={}, resignSuccess={}, resignFailed={}, dbUpdateFailed={}",
                result.totalRead(), result.hiredSuccess(), result.hiredFailed(),
                result.resignSuccess(), result.resignFailed(),
                result.dbUpdateFailed());
        return Result.success(MokaPersonStatusSyncResultVO.from(result));
    }
}
