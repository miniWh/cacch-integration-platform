package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.entity.ihr.OrganizationsdepartmentDO;
import com.cacch.integration.entity.ihr.PersondetailDO;
import com.cacch.integration.entity.moka.MokaPersonDO;
import com.cacch.integration.manager.moka.api.IMokaPersonSyncManager;
import com.cacch.integration.mapper.ihr.OrganizationsdepartmentMapper;
import com.cacch.integration.mapper.ihr.PersondetailMapper;
import com.cacch.integration.service.moka.api.IMokaPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Moka 人员同步编排实现 —— persondetail 外部表 → organizationsdepartment 关联
 * → Moka 人员中间表 upsert
 *
 * <p>调用链：Controller → syncFromIhr() → PersondetailMapper.selectAll()
 * （读外部表 persondetail，@Select 手写 SQL）→
 * OrganizationsdepartmentMapper.selectByDepartmentIds()
 * （批量查外部表 organizationsdepartment.departmentcode）→
 * 内存 Map 匹配 → 字段映射 → IMokaPersonService.batchUpsert()（DB 写）。</p>
 *
 * <p>事务策略：Manager 层不在外层包裹 {@code @Transactional}。
 * 原因：
 * <ul>
 *     <li>persondetail / organizationsdepartment 均为外部表，只读不写</li>
 *     <li>批量 upsert 的事务由 {@link IMokaPersonService#batchUpsert} 内部显式声明
 *     （PROPAGATION.REQUIRED, timeout=120s）</li>
 * </ul>
 * </p>
 *
 * <p>roleId 两阶段策略：Manager 层不赋值 roleId，由 Service 层兜底 DEFAULT 223379。
 * 阶段二角色拉取完成后，此处补充 jobTitle → roleId 匹配逻辑（persondetail
 * 当前无 jobTitle 字段，待业务确认是否有对应字段）。</p>
 *
 * <p>deactivated 规则：staffStatus = 'QUIT' 时置 1，其余置 0。
 * Moka API 固定参数：uniqueType=phone / autoActivated=0 /
 * updateDepartment=false / updateSuperiorEmail=false / thirdPartyId 空
 * （接口 2 推送时使用）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaPersonSyncManagerImpl implements IMokaPersonSyncManager {

    private static final String BIZ = "Moka 人员同步";

    private final PersondetailMapper persondetailMapper;
    private final OrganizationsdepartmentMapper organizationsdepartmentMapper;
    private final IMokaPersonService mokaPersonService;

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

        // —— 2. 收集 departmentId，批量查 organizationsdepartment.departmentcode ——
        Set<String> deptIds = new LinkedHashSet<>();
        for (PersondetailDO p : persondetails) {
            if (p == null) {
                log.info("【{}】persondetail 返回列表含 null 元素, 跳过", BIZ);
                continue;
            }
            if (StringUtils.hasText(p.getDepartmentId())) {
                deptIds.add(p.getDepartmentId());
            }
        }

        Map<String, String> deptCodeMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<OrganizationsdepartmentDO> matchedDepts =
                    organizationsdepartmentMapper.selectByDepartmentIds(new ArrayList<>(deptIds));
            if (!CollectionUtils.isEmpty(matchedDepts)) {
                for (OrganizationsdepartmentDO d : matchedDepts) {
                    if (StringUtils.hasText(d.getDepartmentId())) {
                        deptCodeMap.put(d.getDepartmentId(), d.getDepartmentcode());
                    }
                }
            }
            long unmatchedDeptIds = deptIds.stream()
                    .filter(id -> !deptCodeMap.containsKey(id))
                    .count();
            if (unmatchedDeptIds > 0) {
                log.info("【{}】organizationsdepartment 中未匹配到 departmentcode 的 departmentId 数量={}",
                        BIZ, unmatchedDeptIds);
            }
        }

        // —— 3. 字段映射 ——
        List<MokaPersonDO> batch = new ArrayList<>(totalFetched);
        int deptCodeSkipped = 0;
        int invalidSkipped = 0;

        for (PersondetailDO p : persondetails) {
            if (p == null) {
                log.info("【{}】persondetail 返回列表含 null 元素, 跳过", BIZ);
                continue;
            }
            // userId 为空：无业务主键无法 upsert
            if (!StringUtils.hasText(p.getUserId())) {
                invalidSkipped++;
                log.info("【{}】跳过无 userId（persondetail.id）的员工记录", BIZ);
                continue;
            }

            // department_code 关联
            String deptCode = null;
            if (StringUtils.hasText(p.getDepartmentId())) {
                deptCode = deptCodeMap.get(p.getDepartmentId());
                if (!StringUtils.hasText(deptCode)) {
                    log.info("【{}】员工 departmentId 在 organizationsdepartment 未匹配到 departmentcode, " +
                                    "userId={}, departmentId={}",
                            BIZ, p.getUserId(), p.getDepartmentId());
                    deptCodeSkipped++;
                }
            }

            MokaPersonDO personDO = new MokaPersonDO();
            personDO.setUserId(p.getUserId());
            personDO.setEmployeeNo(p.getEmployeeNo());
            personDO.setUserName(p.getUserName());
            personDO.setNickname(StringUtils.hasText(p.getNickname()) ? p.getNickname() : p.getUserName());
            personDO.setCompanyEmail(p.getCompanyEmail());
            personDO.setContactPhone(p.getContactPhone());
            // roleId：Manager 层不赋值，Service 层兜底 DEFAULT 223379
            personDO.setDepartmentCode(deptCode);
            // superior_email：Moka API updateSuperiorEmail=false，阶段一不填充
            personDO.setEmployeeStatus(p.getEmployeeStatus());
            personDO.setDeactivated(isQuit(p.getEmployeeStatus()) ? 1 : 0);
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

    /**
     * 根据 staffStatus 判断是否离职（Moka API deactivated 字段）
     *
     * <p>规则：staffStatus = 'QUIT' 时置 1，其余（含 IN_SERVICE / null / 空串）置 0。
     * Moka API deactivated 默认值 0，首次创建即便是离职员工也先创建再禁用。</p>
     */
    private boolean isQuit(String staffStatus) {
        return "QUIT".equalsIgnoreCase(staffStatus);
    }
}
