package com.cacch.integration.controller.ihr;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.ihr.request.DeptStaffRequest;
import com.cacch.integration.manager.ihr.api.IIhrPersonManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * IHR 人员查询 REST 接口
 *
 * <p>对外暴露按部门树递归查询在职员工的查询接口。{@code ihrDeptId} 在本 Controller 内写死，
 * 不在请求体中暴露，调用方仅需可选传入 {@code lastUpdateDate} 做时间过滤。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/ihr/persons")
@RequiredArgsConstructor
public class IhrPersonController {

    /**
     * 新河部门 ID — 业务上作为查询根节点
     */
    private static final String FIXED_IHR_DEPT_ID = "396";

    private final IIhrPersonManager ihrPersonManager;

    /**
     * 按部门树递归查询在职员工
     *
     * <p>执行流程：
     * <ol>
     *     <li>从写死的 {@code FIXED_IHR_DEPT_ID} 作为根节点，递归向下查所有 ENABLE 状态子部门</li>
     *     <li>JOIN persondetail 取部门树下所有在职员工（{@code staffStatus = 'IN_SERVICE'}）</li>
     *     <li>LEFT JOIN ihr_staff_update_sync_record 附加 {@code last_update} 字段</li>
     *     <li>{@code lastUpdateDate} 非空时追加时间过滤：{@code last_update >= lastUpdateDate}</li>
     * </ol>
     *
     * <p>请求体可空：
     * <ul>
     *     <li>不传或 {@code lastUpdateDate} 为 null — 返回全部在职员工</li>
     *     <li>传入 {@code lastUpdateDate} — 仅返回 {@code last_update >= 该日期} 的员工</li>
     * </ul>
     *
     * <p>返回 {@code List<Map<String, Object>>}：每个 Map 含 persondetail 表所有字段（原列名 camelCase 保留）
     * + 部门字段（{@code department_name} / {@code department_code}）+ {@code staff_last_update}。</p>
     *
     * @param request 可选请求体，含 {@code lastUpdateDate}（格式 yyyy-MM-dd）；可为 null
     * @return 员工记录列表；无数据时返回空列表（非 null）
     */
    @PostMapping("/by-dept-tree")
    public Result<List<Map<String, Object>>> listStaffByDeptTree(@RequestBody(required = false) DeptStaffRequest request) {
        java.time.LocalDate lastUpdateDate = request == null ? null : request.getLastUpdateDate();
        log.info("【IhrPerson】查询部门树员工, ihrDeptId={}, lastUpdateDate={}", FIXED_IHR_DEPT_ID, lastUpdateDate);
        List<Map<String, Object>> result = ihrPersonManager.listStaffByDeptTree(FIXED_IHR_DEPT_ID, lastUpdateDate);
        log.info("【IhrPerson】查询完成, ihrDeptId={}, count={}", FIXED_IHR_DEPT_ID, result.size());
        return Result.success(result);
    }
}
