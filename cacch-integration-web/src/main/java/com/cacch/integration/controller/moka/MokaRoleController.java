package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.moka.vo.MokaRoleSyncResultVO;
import com.cacch.integration.manager.moka.api.IMokaRoleSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moka 自定义角色 REST 接口
 *
 * <p>当前仅暴露一个接口：角色同步 —— 由 ESB 定时调度触发，
 * 从 Moka 开放平台拉取全量自定义角色并 upsert 到本地 PG 中间表
 * （{@code t_integration_moka_role}）。同步过程为同步阻塞，建议在低峰期执行。</p>
 *
 * <p>鉴权：Moka API Key 由配置文件 {@code moka.api-key}
 * （经环境变量 {@code MOKA_API_KEY} 注入）注入，调用方无需传递密钥。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/moka/roles")
@RequiredArgsConstructor
public class MokaRoleController {

    private final IMokaRoleSyncManager mokaRoleSyncManager;

    /**
     * 从 Moka 开放平台拉取全量自定义角色并落库本地中间表
     *
     * <p>执行流程：
     * <ol>
     *     <li>调用 Moka 角色查询接口（#-75）拉取全量自定义角色</li>
     *     <li>校验响应（code=0/200 为成功）</li>
     *     <li>DTO → DO 字段映射</li>
     *     <li>以 role_id 为业务主键批量 upsert 到
     *         {@code t_integration_moka_role} 表</li>
     * </ol>
     *
     * <p>请求体为空（{} 或不传）即可触发；同步过程为同步阻塞，预计耗时 ≤ 5s
     * （Moka 侧自定义角色数量通常 ≤ 50）。</p>
     *
     * @return 同步执行结果，含 Moka 侧返回总数 / DB upsert 成功数 / 跳过数
     */
    @PostMapping("/sync-from-moka")
    public Result<MokaRoleSyncResultVO> syncFromMoka() {
        log.info("【MokaRoleSyncFromMoka】开始执行 Moka 开放平台 → 本地 PG 角色同步");
        IMokaRoleSyncManager.MokaRoleSyncResult result = mokaRoleSyncManager.syncFromMoka();
        log.info("【MokaRoleSyncFromMoka】同步完成, totalFetched={}, upserted={}, skipped={}",
                result.totalFetched(), result.upserted(), result.skipped());
        return Result.success(MokaRoleSyncResultVO.from(result));
    }
}
