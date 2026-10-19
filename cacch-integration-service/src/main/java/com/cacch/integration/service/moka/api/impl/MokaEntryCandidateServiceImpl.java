package com.cacch.integration.service.moka.api.impl;

import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.common.enums.moka.MokaEntrySyncStatusEnum;
import com.cacch.integration.entity.moka.MokaEntryApplicationDO;
import com.cacch.integration.integration.moka.client.MokaCandidatesClient;
import com.cacch.integration.integration.moka.client.MokaJobClient;
import com.cacch.integration.integration.moka.client.MokaOfferClient;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse.ApplicationItem;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoResponse.OfferExtendInfo;
import com.cacch.integration.mapper.moka.*;
import com.cacch.integration.service.moka.api.IMokaEntryCandidateService;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Moka 待入职候选人服务实现
 *
 * <p>Step 1a：分页拉取 + 4 表幂等 upsert（按 application_id）
 * Step 1b：按 jobId 去重查职位详情，回写 moka_department_code
 * Step 1c：按 applicationId 批量查 Offer 扩展，回写 check_in_date / offer_department_code</p>
 *
 * <p>事务说明：每个 ApplicationItem 的 4 表 upsert 在同一批次内，
 * 批次内同事务保证 1:1 一致性；批次之间由 Manager 层控制事务边界。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaEntryCandidateServiceImpl implements IMokaEntryCandidateService {

    private static final String BIZ = "MokaEntryCandidateService";

    private final MokaCandidatesClient candidatesClient;
    private final MokaJobClient mokaJobClient;
    private final MokaOfferClient mokaOfferClient;

    private final MokaEntryApplicationMapper applicationMapper;
    private final MokaEntryCandidateBaseMapper candidateBaseMapper;
    private final MokaEntryGraduateSchoolMapper graduateSchoolMapper;
    private final MokaEntryCitizenMapper citizenMapper;
    private final MokaEntrySyncMapper syncMapper;

    // ========== Step 1a ==========

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 120)
    public List<MokaEntryApplicationDO> syncAndUpsert(String batch) {
        log.info("【{}】Step1a 开始, batch={}", BIZ, batch);

        List<MokaEntryApplicationDO> allApplications = new ArrayList<>();
        String next = null;
        int page = 0;

        do {
            page++;
            MokaApplicationListRequest request = buildEntryListRequest(next);
            MokaApplicationListResponse response;

            try {
                response = candidatesClient.listApplications(request);
            } catch (Exception e) {
                log.info("【{}】Step1a 分页拉取终止, page={}, reason={}", BIZ, page, e.getMessage());
                break;
            }

            if (response == null || response.getData() == null
                    || response.getData().getApplicationList() == null
                    || response.getData().getApplicationList().isEmpty()) {
                log.info("【{}】Step1a 分页拉取终止, page={}, 无更多数据", BIZ, page);
                break;
            }

            List<ApplicationItem> items = response.getData().getApplicationList();
            log.info("【{}】Step1a 分页拉取, page={}, size={}", BIZ, page, items.size());

            for (ApplicationItem item : items) {
                MokaEntryApplicationDO app = upsertAll(item, batch);
                if (app != null) {
                    allApplications.add(app);
                }
            }

            next = response.getData().getNext();
        } while (next != null && !next.isEmpty());

        log.info("【{}】Step1a 完成, total={}", BIZ, allApplications.size());
        return allApplications;
    }

    /**
     * 按固定参数构造 list_by_condition 请求体
     */
    private MokaApplicationListRequest buildEntryListRequest(String next) {
        MokaApplicationListRequest request = new MokaApplicationListRequest();
        // 固定待入职流程 + 阶段（DTO 字段为 List<Long>，常量为 int，需显式转 Long）
        request.setPipelineIds(List.of((long) MokaConstants.PIPELINE_ID_ENTRY_1, (long) MokaConstants.PIPELINE_ID_ENTRY_2));
        request.setStageIds(List.of((long) MokaConstants.STAGE_ID_ENTRY));
        // 不传 archived，Moka 默认返回未归档
        request.setLimit(20);
        if (next != null && !next.isEmpty()) {
            request.setNext(next);
        }
        return request;
    }

    /**
     * 对单条 ApplicationItem 执行 4 表 upsert + 状态表初始化
     */
    private MokaEntryApplicationDO upsertAll(ApplicationItem item, String batch) {
        if (item.getApplicationId() == null) {
            log.info("【{}】Step1a 跳过, applicationId 为空", BIZ);
            return null;
        }

        Integer appId = item.getApplicationId();
        Long appPid = IdWorker.getId();

        // 1. 主表 upsert
        applicationMapper.upsert(
                appPid,
                appId,
                item.getCandidateBaseInfo() != null ? item.getCandidateBaseInfo().getCandidateId() : null,
                item.getHireMode(),
                toLocalDateTime(item.getAppliedAt()),
                toLocalDateTime(item.getMovedAt()),
                toLocalDateTime(item.getCreatedAt()),
                toLocalDateTime(item.getUpdatedAt()),
                item.getJob() != null ? item.getJob().getJobId() : null,
                item.getJob() != null ? item.getJob().getJobTitle() : null,
                item.getPipeline() != null ? item.getPipeline().getPipelineId() : null,
                item.getPipeline() != null ? item.getPipeline().getName() : null,
                item.getStage() != null ? item.getStage().getStageId() : null,
                item.getStage() != null ? item.getStage().getName() : null,
                item.getStage() != null ? item.getStage().getStageType() : null,
                item.getOwnerInfo() != null ? item.getOwnerInfo().getOwnerId() : null,
                item.getOwnerInfo() != null ? item.getOwnerInfo().getOwnerName() : null,
                item.getArchived() != null ? (item.getArchived() ? 1 : 0) : null,
                item.getArchiveReasons() != null ? item.getArchiveReasons().getId() : null,
                item.getArchiveReasons() != null ? item.getArchiveReasons().getType() : null,
                item.getArchiveReasons() != null ? item.getArchiveReasons().getName() : null,
                item.getArchiveReasons() != null ? toLocalDateTime(item.getArchiveReasons().getArchivedAt()) : null,
                batch
        );

        // 2. candidate_base upsert
        if (item.getCandidateBaseInfo() != null) {
            var cb = item.getCandidateBaseInfo();
            Long cbPid = IdWorker.getId();
            candidateBaseMapper.upsert(
                    cbPid, appId,
                    cb.getName(), cb.getGender(), cb.getAge(),
                    toLocalDateTime(cb.getBirthDate()), cb.getNationality(), cb.getExperience(),
                    toLocalDateTime(cb.getCreatedAt()), cb.getArchived(), toLocalDateTime(cb.getArchivedAt()),
                    cb.getIsBlacklist(),
                    cb.getContactInfo() != null ? cb.getContactInfo().getPhone() : null,
                    cb.getContactInfo() != null ? cb.getContactInfo().getEmail() : null,
                    cb.getContactInfo() != null ? cb.getContactInfo().getCountryCallingCode() : null
            );
        }

        // 3. graduate_school upsert
        if (item.getCandidateBaseInfo() != null && item.getCandidateBaseInfo().getGraduateSchool() != null) {
            var gs = item.getCandidateBaseInfo().getGraduateSchool();
            Long gsPid = IdWorker.getId();
            graduateSchoolMapper.upsert(
                    gsPid, appId,
                    gs.getLastSpeciality(), gs.getLastSchool(),
                    gs.getGraduateDate(), gs.getAcademicDegree(),
                    gs.getSchoolCityId()
            );
        }

        // 4. citizen upsert
        if (item.getCandidateBaseInfo() != null && item.getCandidateBaseInfo().getCitizen() != null) {
            var ct = item.getCandidateBaseInfo().getCitizen();
            Long ctPid = IdWorker.getId();
            citizenMapper.upsert(
                    ctPid, appId,
                    ct.getCitizenId(), ct.getCertificateType(),
                    ct.getIdCardSignOrganization(), ct.getIdCardValidity()
            );
        }

        // 5. 状态表 upsert（初始化 PENDING）
        Long syncPid = IdWorker.getId();
        syncMapper.upsert(syncPid, appId, MokaEntrySyncStatusEnum.PENDING.getCode());

        // 构造返回对象（供后续 Step 1b/1c 使用）
        MokaEntryApplicationDO result = new MokaEntryApplicationDO();
        result.setId(appPid);
        result.setApplicationId(appId);
        result.setMokaJobId(item.getJob() != null ? item.getJob().getJobId() : null);
        return result;
    }

    // ========== Step 1b ==========

    @Override
    public int enrichDepartmentCode(List<MokaEntryApplicationDO> applications) {
        log.info("【{}】Step1b 开始, applications.size={}", BIZ, applications.size());

        // 按候选人 hireMode 分组 jobId（1→social, 2→campus）
        Map<String, Set<String>> hireModeJobIds = new java.util.HashMap<>();
        for (MokaEntryApplicationDO app : applications) {
            if (app.getMokaJobId() == null || app.getMokaJobId().isEmpty()) {
                continue;
            }
            String hireMode = app.getHireMode() != null && app.getHireMode() == 2 ? "campus" : "social";
            hireModeJobIds.computeIfAbsent(hireMode, k -> new HashSet<>()).add(app.getMokaJobId());
        }

        if (hireModeJobIds.isEmpty()) {
            log.info("【{}】Step1b 跳过, reason=无有效jobId", BIZ);
            return 0;
        }

        // 按 hireMode 分组查职位，合并结果
        Map<String, String> jobDeptMap = new java.util.HashMap<>();
        try {
            for (Map.Entry<String, Set<String>> entry : hireModeJobIds.entrySet()) {
                Map<String, String> partial = mokaJobClient.getDepartmentCodeByJobIds(
                        new ArrayList<>(entry.getValue()), entry.getKey());
                jobDeptMap.putAll(partial);
            }
        } catch (Exception e) {
            log.info("【{}】Step1b 终止, reason={}", BIZ, e.getMessage());
            return 0;
        }

        // 回写主表
        int updated = 0;
        for (MokaEntryApplicationDO app : applications) {
            if (app.getMokaJobId() == null) {
                continue;
            }
            String deptCode = jobDeptMap.get(app.getMokaJobId());
            if (deptCode != null) {
                int rows = applicationMapper.updateDepartmentCodeByApplicationId(app.getApplicationId(), deptCode);
                if (rows > 0) {
                    updated++;
                }
            } else {
                log.info("【{}】Step1b jobId 未匹配到部门编码, applicationId={}, jobId={}",
                        BIZ, app.getApplicationId(), app.getMokaJobId());
            }
        }

        log.info("【{}】Step1b 完成, updated={}", BIZ, updated);
        return updated;
    }

    // ========== Step 1c ==========

    @Override
    public int enrichOfferFields(List<MokaEntryApplicationDO> applications) {
        log.info("【{}】Step1c 开始, applications.size={}", BIZ, applications.size());

        // 收集 applicationId
        List<Long> appIds = applications.stream()
                .map(a -> (long) a.getApplicationId())
                .collect(Collectors.toList());

        if (appIds.isEmpty()) {
            log.info("【{}】Step1c 跳过, reason=无有效applicationId", BIZ);
            return 0;
        }

        // 批量查 offer 扩展 → Map<applicationId, OfferExtendInfo>
        Map<Long, OfferExtendInfo> offerMap;
        try {
            offerMap = mokaOfferClient.getOfferExtendInfos(appIds);
        } catch (Exception e) {
            log.info("【{}】Step1c 终止, reason={}", BIZ, e.getMessage());
            return 0;
        }

        // 回写主表
        int updated = 0;
        for (MokaEntryApplicationDO app : applications) {
            OfferExtendInfo info = offerMap.get((long) app.getApplicationId());
            if (info == null) {
                log.info("【{}】Step1c 未查到 offer 扩展, applicationId={}", BIZ, app.getApplicationId());
                continue;
            }

            LocalDateTime checkInDate = parseCheckInDate(info.getCheckInDate());
            String deptCode = info.getHcmDepartmentId() != null ? String.valueOf(info.getHcmDepartmentId()) : null;
            int rows = applicationMapper.updateOfferFieldsByApplicationId(
                    app.getApplicationId(),
                    checkInDate,
                    deptCode,
                    info.getHcmDepartmentName(),
                    info.getStatus()
            );
            if (rows > 0) {
                updated++;
            }
        }

        log.info("【{}】Step1c 完成, updated={}", BIZ, updated);
        return updated;
    }

    // ========== 时间转换工具 ==========

    /**
     * Moka 毫秒时间戳 → LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(epochMillis),
                java.time.ZoneId.systemDefault());
    }

    /**
     * Offer checkInDate 字符串 → LocalDateTime
     *
     * <p>Moka 返回格式：ISO8601（如 {@code 2018-09-20T13:00:07.000Z}），
     * 也可能是 yyyy-MM-dd。兜底两种格式。</p>
     */
    private LocalDateTime parseCheckInDate(String checkInDateStr) {
        if (checkInDateStr == null || checkInDateStr.isEmpty()) {
            return null;
        }
        try {
            // ISO8601 格式
            return OffsetDateTime.parse(checkInDateStr).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            // yyyy-MM-dd 格式
            return java.time.LocalDate.parse(checkInDateStr, DateTimeFormatter.ISO_DATE).atStartOfDay();
        } catch (Exception ignored) {
        }
        log.info("【{}】Step1c checkInDate 格式无法解析, value={}", BIZ, checkInDateStr);
        return null;
    }
}
