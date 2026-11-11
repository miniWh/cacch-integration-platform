package com.cacch.integration.controller.ihr;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicInfo;
import com.cacch.integration.service.ihr.api.IIhrStaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * IHR 员工域测试端点 — 员工类所有接口测试端点的聚合 Controller
 *
 * <p>所有端点路由以 {@code /api/v1/test/ihr/staffs} 为前缀，与正式业务 Controller
 * （如 {@code IhrStaffController}，路由 {@code /api/v1/ihr/staffs}）完全隔离。
 * 测试完成后可直接删除本类，不影响正式路由。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/test/ihr/staffs")
@RequiredArgsConstructor
public class TestIhrStaffController {

    private final IIhrStaffService ihrStaffService;

    /**
     * 测试端点：全量拉取 IHR 员工 ID 清单
     *
     * <p>内部自动循环翻页直到末页，一次返回完整 ID 列表。</p>
     *
     * @param staffStatus 员工状态过滤（可选）：
     *                    <ul>
     *                        <li>{@code IN_SERVICE} — 仅在职</li>
     *                        <li>{@code QUIT} — 仅离职</li>
     *                        <li>不传 — 全部状态</li>
     *                    </ul>
     * @return 合并后的员工 ID 列表；无数据时返回空列表（不会返回 null）
     */
    @GetMapping("/ids")
    public Result<List<String>> fetchAllStaffIds(
            @RequestParam(required = false) String staffStatus) {
        log.info("【IHR员工域测试】收到获取员工ID清单请求, staffStatus={}", staffStatus);
        return Result.success(ihrStaffService.fetchAllStaffIds(staffStatus));
    }

    /**
     * 测试端点：按 staffId 查询单个员工的基本信息
     *
     * @param staffId 员工 ID
     * @return 员工基本资料；iHR 响应无 data 时返回 null
     */
    @GetMapping("/{staffId}/basic")
    public Result<IhrStaffBasicInfo> fetchStaffBasic(@PathVariable String staffId) {
        log.info("【IHR员工域测试】收到获取员工基本信息请求, staffId={}", staffId);
        return Result.success(ihrStaffService.fetchStaffBasic(staffId));
    }
}
