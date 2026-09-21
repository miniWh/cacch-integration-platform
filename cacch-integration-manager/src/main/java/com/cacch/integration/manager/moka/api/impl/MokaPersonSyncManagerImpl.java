package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.entity.ihr.IhrDepartmentDO;
import com.cacch.integration.entity.ihr.PersondetailDO;
import com.cacch.integration.entity.moka.MokaPersonDO;
import com.cacch.integration.manager.moka.api.IMokaPersonSyncManager;
import com.cacch.integration.mapper.ihr.PersondetailMapper;
import com.cacch.integration.service.ihr.api.IIhrDepartmentService;
import com.cacch.integration.service.moka.api.IMokaPersonService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Moka 人员同步编排实现 —— persondetail 外部表 → ihr_department 关联 →
 * Moka 人员中间表 upsert
 *
 * <p>调用链：Controller → syncFromIhr() → PersondetailMapper.selectAll()
 * （读外部表，@Select 手写 SQL 避免 MP 自动条件）→
 * IIhrDepartmentService.listByIhrDeptIds()（批量 IN 查询 department_code）
 * → 内存 Map 匹配 → 字段映射 → IMokaPersonService.batchUpsert()（DB 写）。</p>
 *
 * <p>事务策略：Manager 层不在外层包裹 {@code @Transactional}。
 * 原因：
 * <ul>
 *     <li>persondetail 为外部表，只读不写，无需事务保护</li>
 *     <li>Department 关联为本地表只读查询</li>
 *     <li>批量 upsert 的事务由 {@link IMokaPersonService#batchUpsert} 内部显式声明
 *     （PROPAGATION.REQUIRED, timeout=120s）</li>
 * </ul>
 * </p>
 *
 * <p>roleId 两阶段策略：Manager 层不赋值 roleId，由 Service 层兜底 DEFAULT 223379。
 * 阶段二拉取角色后，此处补充 jobTitle → roleId 匹配逻辑。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaPersonSyncManagerImpl implements IMokaPersonSyncManager {

    private static final String BIZ = "Moka 人员同步";

    private final PersondetailMapper persondetailMapper;
    private final IIhrDepartmentService ihrDepartmentService;
    private final IMokaPersonService mokaPersonService;

    /**
     * JSON 工具 —— 用于防御性解析 persondetail.superiorsInfo
     * （结构未知时不抛异常，仅置 superior_email 为 null）
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public MokaPersonSyncResult syncFromIhr() {
        log.info("【{}】开始从 persondetail 外部表同步员工到 Moka 人员中间表", BIZ);

        // —— 1. 读 persondetail 全量 ——
        List<PersondetailDO> persondetails = persondetailMapper.selectAll();
        int totalFetched = persondetails == null ? 0 : persondetails.size();
        log.info("【{}】persondetail 返回员工总数={}", BIZ, totalFetched);

        if (totalFetched == 0) {
            log.info("【{}】persondetail 无数据, 同步结束", BIZ);
            return new MokaPersonSyncResult(0, 0, 0, 0);
        }

        // —— 2. 收集 departmentId，批量查 department_code ——
        Set<String> deptIds = new LinkedHashSet<>();
        for (PersondetailDO p : persondetails) {
            if (StringUtils.hasText(p.getDepartmentId())) {
                deptIds.add(p.getDepartmentId());
            }
        }
        List<IhrDepartmentDO> matchedDepts = deptIds.isEmpty()
                ? Collections.emptyList()
                : ihrDepartmentService.listByIhrDeptIds(new ArrayList<>(deptIds));

        // 构建 Map: ihrDeptId → departmentCode
        Map<String, String> deptCodeMap = new HashMap<>(matchedDepts.size());
        for (IhrDepartmentDO d : matchedDepts) {
            if (d.getIhrDeptId() != null) {
                deptCodeMap.put(d.getIhrDeptId(), d.getDepartmentCode());
            }
        }

        // 记录匹配缺失数（用于日志）
        long unmatchedDeptIds = deptIds.stream()
                .filter(id -> !deptCodeMap.containsKey(id))
                .count();
        if (unmatchedDeptIds > 0) {
            log.info("【{}】ihr_department 中未匹配到的 departmentId 数量={}", BIZ, unmatchedDeptIds);
        }

        // —— 3. 字段映射 ——
        List<MokaPersonDO> batch = new ArrayList<>(totalFetched);
        int deptCodeSkipped = 0;
        int invalidSkipped = 0;

        for (PersondetailDO p : persondetails) {
            // userId 为空：无业务主键无法 upsert
            if (!StringUtils.hasText(p.getUserId())) {
                invalidSkipped++;
                log.info("【{}】跳过无 userId 的员工记录", BIZ);
                continue;
            }

            // department_code 关联
            String deptCode = null;
            if (StringUtils.hasText(p.getDepartmentId())) {
                deptCode = deptCodeMap.get(p.getDepartmentId());
                if (!StringUtils.hasText(deptCode)) {
                    // 关联不到 → 置 null 但不跳过（允许员工存在 department_code 为空的中间表记录）
                    log.info("【{}】员工 departmentId 在 ihr_department 未匹配到 department_code, userId={}, departmentId={}",
                            BIZ, p.getUserId(), p.getDepartmentId());
                    deptCodeSkipped++;
                }
            }

            MokaPersonDO personDO = new MokaPersonDO();
            personDO.setUserId(p.getUserId());
            personDO.setEmployeeNo(p.getEmployeeNo());
            personDO.setUserName(p.getName());
            personDO.setNickname(StringUtils.hasText(p.getNickname()) ? p.getNickname() : p.getName());
            personDO.setCompanyEmail(p.getCompanyEmail());
            personDO.setContactPhone(p.getPhone());
            // roleId：Manager 层不赋值，Service 层兜底 DEFAULT 223379
            personDO.setDepartmentCode(deptCode);
            personDO.setSuperiorEmail(extractSuperiorEmail(p.getSuperiorsInfo()));
            personDO.setEmployeeStatus(p.getEmployeeStatus());
            personDO.setDeactivated(isDeactivated(p.getEmployeeStatus()) ? 1 : 0);
            personDO.setLocale("zh-CN");
            personDO.setTimezone("Asia/Shanghai");
            personDO.setMokaSyncStatus(0); // PENDING

            batch.add(personDO);
        }

        if (batch.isEmpty()) {
            log.info("【{}】全部员工记录被跳过, totalFetched={}, invalidSkipped={}",
                    BIZ, totalFetched, invalidSkipped);
            return new MokaPersonSyncResult(totalFetched, 0, deptCodeSkipped, invalidSkipped);
        }

        // —— 4. 批量 upsert 落库 ——
        log.info("【{}】开始批量 upsert 落库, totalFetched={}, validToUpsert={}, deptCodeSkipped={}, invalidSkipped={}",
                BIZ, totalFetched, batch.size(), deptCodeSkipped, invalidSkipped);

        int personUpserted = mokaPersonService.batchUpsert(batch);

        log.info("【{}】同步完成, totalFetched={}, personUpserted={}, deptCodeSkipped={}, invalidSkipped={}",
                BIZ, totalFetched, personUpserted, deptCodeSkipped, invalidSkipped);

        return new MokaPersonSyncResult(totalFetched, personUpserted, deptCodeSkipped, invalidSkipped);
    }

    // —— 辅助方法 ——

    /**
     * 从 persondetail.superiorsInfo 提取直属领导邮箱
     *
     * <p>防御性实现，适配多种可能格式：
     * <ul>
     *     <li>JSON 数组：{@code [{"email":"a@b.com","level":1}, ...]} → 取第一个元素的 email</li>
     *     <li>JSON 对象：{@code {"email":"a@b.com"}} → 取 email</li>
     *     <li>空串 / null → 返回 null</li>
     * </ul>
     * 任何解析异常均吞掉并返回 null，不阻塞主流程。</p>
     *
     * <p><strong>待联调验证</strong>：persondetail.superiorsInfo 实际结构需连库确认后完善解析规则。</p>
     *
     * @param superiorsInfo persondetail.superiorsInfo 原始值
     * @return 直属领导邮箱；提取失败时返回 null
     */
    private String extractSuperiorEmail(String superiorsInfo) {
        if (!StringUtils.hasText(superiorsInfo)) {
            return null;
        }
        try {
            String trimmed = superiorsInfo.trim();
            JsonNode root = OBJECT_MAPPER.readTree(trimmed);

            // 数组形式：取第一个元素的 email 字段
            if (root.isArray() && !root.isEmpty()) {
                JsonNode first = root.get(0);
                if (first.has("email")) {
                    return first.get("email").asText();
                }
            }
            // 对象形式：直接取 email 字段
            if (root.isObject() && root.has("email")) {
                return root.get("email").asText();
            }
        } catch (Exception e) {
            // JSON 解析失败（可能不是 JSON 格式），吞掉异常
            log.info("【{}】superiorsInfo 解析失败, 返回 null, 原始值前缀={}",
                    BIZ, superiorsInfo.length() > 50 ? superiorsInfo.substring(0, 50) : superiorsInfo);
        }
        return null;
    }

    /**
     * 根据员工状态判断是否禁用（Moka API deactivated 字段）
     *
     * <p>当前规则：employeeStatus 包含 "离职" / "RESIGN" / "DISABLE" 关键字时置 1。
     * 其他情况置 0。需联调确认 persondetail.employeeStatus 的实际枚举值。</p>
     */
    private boolean isDeactivated(String employeeStatus) {
        if (!StringUtils.hasText(employeeStatus)) {
            return false;
        }
        String upper = employeeStatus.toUpperCase();
        return upper.contains("RESIGN") || upper.contains("DISABLE")
                || employeeStatus.contains("离职");
    }
}
