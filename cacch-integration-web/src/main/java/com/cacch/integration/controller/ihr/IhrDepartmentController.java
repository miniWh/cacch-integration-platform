package com.cacch.integration.controller.ihr;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.ihr.IhrOrgConverter;
import com.cacch.integration.dto.ihr.request.SearchDepartmentRequest;
import com.cacch.integration.dto.ihr.vo.DepartmentPageVO;
import com.cacch.integration.dto.ihr.vo.DepartmentVO;
import com.cacch.integration.entity.ihr.IhrDepartmentDO;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.manager.ihr.api.IIhrDeptSyncManager;
import com.cacch.integration.manager.ihr.api.IIhrOrgManager;
import com.cacch.integration.service.ihr.api.IIhrDepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * IHR 开放平台部门查询 REST 接口
 *
 * <p>鉴权：IHR 凭证由配置文件 {@code ihr.app-key} / {@code ihr.app-secret}（经环境变量 IHR_APP_KEY / IHR_APP_SECRET 注入）注入，
 * 调用方无需传递密钥。内部已封装 access_token 缓存与自动续期。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/ihr/departments")
@RequiredArgsConstructor
public class IhrDepartmentController {

    private final IIhrOrgManager ihrOrgManager;
    private final IhrOrgConverter ihrOrgConverter;
    private final IIhrDeptSyncManager ihrDeptSyncManager;
    private final IIhrDepartmentService ihrDepartmentService;

    /**
     * 分页查询 IHR 部门清单（获取部门清单v3）
     *
     * <p>调用方可在 {@code conditions} 中传入 0..N 个搜索条件；空集合视为全量查询。
     * 典型搜索字段：{@code departmentName} / {@code departmentCode} / {@code departmentId}，
     * 搜索类型：{@code EQUAL} / {@code LIKE} / {@code FUZZY} / {@code IN}。</p>
     *
     * @param request 分页与搜索条件，不可为空
     * @return 部门分页视图（含 departments / totalElements / totalPages / end）
     */
    @PostMapping("/search")
    public Result<DepartmentPageVO> searchDepartments(@Valid @RequestBody SearchDepartmentRequest request) {
        log.info("收到部门查询请求, page={}, size={}", request.getPage(), request.getSize());
        IhrOrgSearchRequest upstreamRequest = ihrOrgConverter.toUpstreamRequest(request);
        return Result.success(ihrOrgConverter.toPageVO(ihrOrgManager.searchDepartments(upstreamRequest)));
    }

    /**
     * 手动触发 IHR 部门全量同步 — 拉取全量部门清单并 upsert 到本地快照表 {@code t_integration_ihr_department}
     *
     * <p>同步过程：分页拉取（每页 100 条，批次 100 条落库）→ ON CONFLICT(uuid) DO UPDATE。
     * 同步批次号（sync_batch）自动生成 UUID，便于事后追溯。</p>
     *
     * <p>注意：当前实现未做请求去重或分布式锁（单机串行调用即可；并发触发时最后一次成功的批次将全部覆盖，
     * 但不会破坏数据完整性——ON CONFLICT 保证幂等）。如需定时同步可后续追加 @Scheduled 调用。</p>
     *
     * @return 同步结果摘要（totalFetched / upserted / skipped）
     */
    @PostMapping("/sync")
    public Result<IIhrDeptSyncManager.IhrDeptSyncResult> syncAll() {
        log.info("收到部门全量同步请求");
        return Result.success(ihrDeptSyncManager.syncAll());
    }

    /**
     * 查询 parent_id = '396' 下所有子孙部门（递归，仅 ENABLE 状态、未逻辑删除）
     *
     * <p>使用 PostgreSQL WITH RECURSIVE CTE 递归遍历部门树。
     * 锚点条件：{@code parent_id = '396'}，递归时子记录的 parent_id
     * 关联父记录的 ihr_dept_id。结果按 sequence 升序、id 升序稳定排序。</p>
     *
     * @return 子孙部门视图列表，无数据时返回空列表，不会返回 null
     */
    @GetMapping("/subtree")
    public Result<List<DepartmentVO>> getSubtree() {
        String parentId = "396";
        log.info("收到部门子树查询请求, parentId={}", parentId);
        List<IhrDepartmentDO> deptList = ihrDepartmentService.listRecursiveByParentId(parentId);
        return Result.success(ihrOrgConverter.toVOListFromDO(deptList));
    }
}
