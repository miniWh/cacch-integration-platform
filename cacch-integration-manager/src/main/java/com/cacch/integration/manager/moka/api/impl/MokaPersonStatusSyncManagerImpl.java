package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.ihr.PersondetailDO;
import com.cacch.integration.entity.moka.MokaPersonStatusDO;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationHiredRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest;
import com.cacch.integration.manager.moka.api.IMokaPersonStatusSyncManager;
import com.cacch.integration.mapper.ihr.PersondetailMapper;
import com.cacch.integration.service.moka.api.IMokaApplicationService;
import com.cacch.integration.service.moka.api.IMokaPersonStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Moka 候选人入离职状态同步编排实现
 *
 * <p>调用链：Controller → pushStatusToMoka() →
 * IMokaPersonStatusService.listHiredPending()/listResignPending()
 * （INNER JOIN candidate_base 天然过滤无候选人的员工，JOIN 同时取 application_id）→
 * PersondetailMapper.selectByStaffNos()（按工号关联外部表补日期）→
 * IMokaApplicationService.hireApplication()/resignApplication()（HTTP）→
 * IMokaPersonStatusService.upsertHiredResult()/upsertResignResult()（DB 回写）。</p>
 *
 * <p>事务策略：Manager 层不在外层包裹 {@code @Transactional}。
 * <ul>
 *     <li>Moka hired / resign 为 HTTP 调用，禁止加事务</li>
 *     <li>状态行回写由 {@link IMokaPersonStatusService} 内部显式声明事务
 *     （PROPAGATION.REQUIRED, timeout=30s）</li>
 *     <li>单条失败不阻断其余条目（逐条 try-catch 并计数）</li>
 * </ul>
 * </p>
 *
 * <p>关联机制：
 * <ul>
 *     <li>待处理集合 INNER JOIN t_integration_moka_entry_candidate_base
 *     ON p.contact_phone = c.moka_contact_phone（Moka 域内部关联），
 *     天然排除无候选人记录的员工</li>
 *     <li>application_id 由 candidate_base JOIN 直接取，无需额外解析</li>
 *     <li>iHR/persondetail 关联：employee_no = staffNo（一一对应且唯一）</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaPersonStatusSyncManagerImpl implements IMokaPersonStatusSyncManager {

    private static final String BIZ = "Moka 候选人状态同步";

    /**
     * 同步状态常量：1=SYNCED
     */
    private static final int STATUS_SYNCED = 1;

    /**
     * 同步状态常量：2=SYNC_FAILED
     */
    private static final int STATUS_SYNC_FAILED = 2;

    /**
     * 成功结果摘要
     */
    private static final String RESULT_SUCCESS = "success";

    /**
     * 状态表失败原因列长度上限（VARCHAR(500)），超出截断
     */
    private static final int MAX_ERROR_LENGTH = 500;

    /**
     * hired 接口 hiredAt 时间格式（MokaConstants.HIRED_AT_PATTERN）
     */
    private static final DateTimeFormatter HIRED_AT_FORMATTER =
            DateTimeFormatter.ofPattern(MokaConstants.HIRED_AT_PATTERN);

    /**
     * resign 接口 time 时间格式（MokaConstants.RESIGN_TIME_PATTERN）
     */
    private static final DateTimeFormatter RESIGN_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(MokaConstants.RESIGN_TIME_PATTERN);

    private final IMokaPersonStatusService mokaPersonStatusService;
    private final IMokaApplicationService mokaApplicationService;
    private final PersondetailMapper persondetailMapper;

    @Override
    public MokaPersonStatusSyncResult pushStatusToMoka() {
        log.info("【{}】开始执行候选人入离职状态 → Moka 申请状态同步", BIZ);

        // —— 1. 查询待处理集合（INNER JOIN candidate_base 已天然过滤无候选人）——
        List<MokaPersonStatusDO> hiredPending = mokaPersonStatusService.listHiredPending();
        List<MokaPersonStatusDO> resignPending = mokaPersonStatusService.listResignPending();
        int hiredTotal = CollectionUtils.isEmpty(hiredPending) ? 0 : hiredPending.size();
        int resignTotal = CollectionUtils.isEmpty(resignPending) ? 0 : resignPending.size();
        int totalRead = hiredTotal + resignTotal;
        log.info("【{}】待处理集合读取完成（已 INNER JOIN candidate_base 过滤）, hiredPending={}, resignPending={}, totalRead={}",
                BIZ, hiredTotal, resignTotal, totalRead);

        if (totalRead == 0) {
            log.info("【{}】无待处理记录, 同步结束", BIZ);
            return new MokaPersonStatusSyncResult(0, 0, 0, 0, 0, 0);
        }

        // —— 2. 按工号批量关联 persondetail，取入职 / 离职日期 ——
        Map<String, PersondetailDO> personMap = loadPersondetailMap(hiredPending, resignPending);

        // —— 3. 逐条处理（单条失败不阻断）——
        int hiredSuccess = 0;
        int hiredFailed = 0;
        int resignSuccess = 0;
        int resignFailed = 0;
        int dbUpdateFailed = 0;

        for (MokaPersonStatusDO item : hiredPending) {
            if (item == null || !StringUtils.hasText(item.getEmployeeNo())) {
                log.info("【{}】入职待处理记录缺失工号, 跳过, item={}", BIZ, item);
                continue;
            }
            ItemOutcome outcome = processHired(item, personMap);
            if (outcome.apiSuccess() && outcome.dbWriteOk()) {
                hiredSuccess++;
            }
            if (!outcome.apiSuccess()) {
                hiredFailed++;
            }
            if (!outcome.dbWriteOk()) {
                dbUpdateFailed++;
            }
        }

        for (MokaPersonStatusDO item : resignPending) {
            if (item == null || !StringUtils.hasText(item.getEmployeeNo())) {
                log.info("【{}】离职待处理记录缺失工号, 跳过, item={}", BIZ, item);
                continue;
            }
            ItemOutcome outcome = processResign(item, personMap);
            if (outcome.apiSuccess() && outcome.dbWriteOk()) {
                resignSuccess++;
            }
            if (!outcome.apiSuccess()) {
                resignFailed++;
            }
            if (!outcome.dbWriteOk()) {
                dbUpdateFailed++;
            }
        }

        log.info("【{}】同步完成, totalRead={}, hiredSuccess={}, hiredFailed={}, resignSuccess={}, resignFailed={}, dbUpdateFailed={}",
                BIZ, totalRead, hiredSuccess, hiredFailed, resignSuccess, resignFailed, dbUpdateFailed);

        return new MokaPersonStatusSyncResult(totalRead, hiredSuccess, hiredFailed,
                resignSuccess, resignFailed, dbUpdateFailed);
    }

    /**
     * 按工号批量查询 persondetail 日期字段并构建 Map
     *
     * <p>工号来源：入职 + 离职待处理集合的 employeeNo（= persondetail.staffNo）。
     * 单次 IN 查询避免逐条回表。</p>
     *
     * @param hiredPending  入职待处理集合（可能为 null 或空）
     * @param resignPending 离职待处理集合（可能为 null 或空）
     * @return 工号 → persondetail 记录 Map；无匹配时返回空 Map（非 null）
     */
    private Map<String, PersondetailDO> loadPersondetailMap(List<MokaPersonStatusDO> hiredPending,
                                                            List<MokaPersonStatusDO> resignPending) {
        Set<String> staffNos = new LinkedHashSet<>();
        collectStaffNos(hiredPending, staffNos);
        collectStaffNos(resignPending, staffNos);
        if (staffNos.isEmpty()) {
            log.info("【{}】待处理集合无有效工号, 跳过 persondetail 关联查询", BIZ);
            return Map.of();
        }

        List<PersondetailDO> details = persondetailMapper.selectByStaffNos(new ArrayList<>(staffNos));
        Map<String, PersondetailDO> personMap = new HashMap<>(staffNos.size());
        if (!CollectionUtils.isEmpty(details)) {
            for (PersondetailDO detail : details) {
                if (detail != null && StringUtils.hasText(detail.getEmployeeNo())) {
                    personMap.put(detail.getEmployeeNo(), detail);
                }
            }
        }

        long unmatched = staffNos.stream().filter(no -> !personMap.containsKey(no)).count();
        if (unmatched > 0) {
            log.info("【{}】persondetail 中未匹配到记录的工号数量={}, 关联关系以 employee_no=staffNo 为准",
                    BIZ, unmatched);
        }
        log.info("【{}】persondetail 关联查询完成, staffNos={}, matched={}", BIZ, staffNos.size(), personMap.size());
        return personMap;
    }

    /**
     * 从待处理集合收集有效工号
     *
     * @param items    待处理集合（允许为 null）
     * @param staffNos 工号收集容器（去重保序）
     */
    private void collectStaffNos(List<MokaPersonStatusDO> items, Set<String> staffNos) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        for (MokaPersonStatusDO item : items) {
            if (item != null && StringUtils.hasText(item.getEmployeeNo())) {
                staffNos.add(item.getEmployeeNo());
            }
        }
    }

    /**
     * 处理单条入职同步
     *
     * <p>流程：取 JOIN candidate_base 带回的 applicationId → 取 contractBeginDate
     * 构造 hiredAt（缺失标记 SYNC_FAILED）→ 计算 probation（整月差值 0-6）→
     * 调 Moka hired 接口 → 回写状态行。</p>
     *
     * @param item      入职待处理记录（含 applicationId，由 candidate_base JOIN 直接取）
     * @param personMap 工号 → persondetail 记录 Map
     * @return 单条处理结果
     */
    private ItemOutcome processHired(MokaPersonStatusDO item, Map<String, PersondetailDO> personMap) {
        Integer applicationId = item.getApplicationId();

        PersondetailDO detail = personMap.get(item.getEmployeeNo());
        if (detail == null || detail.getContractBeginDate() == null) {
            String reason = detail == null ? "persondetail 无匹配记录" : "contractBeginDate 为空";
            log.info("【{}】实际入职时间缺失, 标记 SYNC_FAILED, employeeNo={}, applicationId={}, reason={}",
                    BIZ, item.getEmployeeNo(), applicationId, reason);
            return writeHiredStatus(item, false, STATUS_SYNC_FAILED, reason,
                    LocalDateTime.now(), applicationId);
        }

        Integer probation = computeProbation(detail.getContractBeginDate(),
                detail.getProbationEndDate(), item.getEmployeeNo());
        MokaApplicationHiredRequest request = new MokaApplicationHiredRequest();
        request.setHiredAt(detail.getContractBeginDate().format(HIRED_AT_FORMATTER));
        request.setProbation(probation);

        try {
            mokaApplicationService.hireApplication(applicationId, request);
            log.info("【{}】入职同步成功, employeeNo={}, applicationId={}, hiredAt={}, probation={}",
                    BIZ, item.getEmployeeNo(), applicationId, request.getHiredAt(), probation);
            return writeHiredStatus(item, true, STATUS_SYNCED, RESULT_SUCCESS,
                    LocalDateTime.now(), applicationId);
        } catch (BizException e) {
            log.info("【{}】入职同步失败, 标记 SYNC_FAILED, employeeNo={}, applicationId={}, reason={}",
                    BIZ, item.getEmployeeNo(), applicationId, e.getMessage());
            log.error("【{}】Moka hired 调用异常, employeeNo={}, applicationId={}",
                    BIZ, item.getEmployeeNo(), applicationId, e);
            return writeHiredStatus(item, false, STATUS_SYNC_FAILED, truncate(e.getMessage()),
                    LocalDateTime.now(), applicationId);
        }
    }

    /**
     * 处理单条离职同步
     *
     * <p>流程：取 JOIN candidate_base 带回的 applicationId → 构造 resign 请求
     * （detail=固定文案，time=leaveDate，缺失时不传由 Moka 默认取请求时间）→
     * 调 Moka resign 接口 → 回写状态行。</p>
     *
     * @param item      离职待处理记录（含 applicationId，由 candidate_base JOIN 直接取）
     * @param personMap 工号 → persondetail 记录 Map
     * @return 单条处理结果
     */
    private ItemOutcome processResign(MokaPersonStatusDO item, Map<String, PersondetailDO> personMap) {
        Integer applicationId = item.getApplicationId();

        MokaApplicationResignRequest request = new MokaApplicationResignRequest();
        request.setDetail(MokaConstants.RESIGN_DEFAULT_DETAIL);
        PersondetailDO detail = personMap.get(item.getEmployeeNo());
        if (detail != null && detail.getLeaveDate() != null) {
            request.setTime(detail.getLeaveDate().format(RESIGN_TIME_FORMATTER));
        } else {
            log.info("【{}】离职时间为空, resign.time 不传由 Moka 默认取请求时间, employeeNo={}, applicationId={}",
                    BIZ, item.getEmployeeNo(), applicationId);
        }

        try {
            mokaApplicationService.resignApplication(applicationId, request);
            log.info("【{}】离职同步成功, employeeNo={}, applicationId={}, time={}",
                    BIZ, item.getEmployeeNo(), applicationId, request.getTime());
            return writeResignStatus(item, true, STATUS_SYNCED, RESULT_SUCCESS,
                    LocalDateTime.now(), applicationId);
        } catch (BizException e) {
            log.info("【{}】离职同步失败, 标记 SYNC_FAILED, employeeNo={}, applicationId={}, reason={}",
                    BIZ, item.getEmployeeNo(), applicationId, e.getMessage());
            log.error("【{}】Moka resign 调用异常, employeeNo={}, applicationId={}",
                    BIZ, item.getEmployeeNo(), applicationId, e);
            return writeResignStatus(item, false, STATUS_SYNC_FAILED, truncate(e.getMessage()),
                    LocalDateTime.now(), applicationId);
        }
    }

    /**
     * 计算试用期月数 —— hired 接口 probation 数据来源
     *
     * <p>规则：probationEndDate − contractBeginDate 的整月差值；
     * 任一日期为空或差值 ≤ 0 时按 0 处理；差值超过
     * {@link MokaConstants#PROBATION_MAX_MONTHS}（6）时截断为 6。</p>
     *
     * @param contractBeginDate 合同开始日期（实际入职时间），非空
     * @param probationEndDate  试用期结束日期，允许为空
     * @param employeeNo        工号（仅用于日志）
     * @return 试用期月数（0-6）
     */
    private Integer computeProbation(LocalDate contractBeginDate, LocalDate probationEndDate, String employeeNo) {
        if (probationEndDate == null) {
            log.info("【{}】试用期结束日期为空, probation 按 0 处理, employeeNo={}, contractBeginDate={}",
                    BIZ, employeeNo, contractBeginDate);
            return 0;
        }
        long months = ChronoUnit.MONTHS.between(contractBeginDate, probationEndDate);
        if (months <= 0) {
            log.info("【{}】试用期差值 ≤ 0, probation 按 0 处理, employeeNo={}, contractBeginDate={}, probationEndDate={}",
                    BIZ, employeeNo, contractBeginDate, probationEndDate);
            return 0;
        }
        if (months > MokaConstants.PROBATION_MAX_MONTHS) {
            log.info("【{}】试用期差值 {} 超过上限, probation 截断为 {}, employeeNo={}",
                    BIZ, months, MokaConstants.PROBATION_MAX_MONTHS, employeeNo);
            return MokaConstants.PROBATION_MAX_MONTHS;
        }
        return (int) months;
    }

    /**
     * 回写入职同步结果到状态表
     *
     * @param item          待处理记录
     * @param apiSuccess    Moka hired 调用是否成功
     * @param syncStatus    入职同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param error         结果摘要（允许为空）
     * @param syncTime      同步时间
     * @param applicationId Moka 申请 id（来自 candidate_base JOIN）
     * @return 单条处理结果（dbWriteOk 反映回写是否成功）
     */
    private ItemOutcome writeHiredStatus(MokaPersonStatusDO item, boolean apiSuccess, int syncStatus,
                                         String error, LocalDateTime syncTime, Integer applicationId) {
        try {
            int rows = mokaPersonStatusService.upsertHiredResult(item, syncStatus, error, syncTime, applicationId);
            if (rows == 0) {
                log.info("【{}】入职状态行回写影响 0 行, employeeNo={}, syncStatus={}",
                        BIZ, item.getEmployeeNo(), syncStatus);
                return new ItemOutcome(false, false);
            }
            return new ItemOutcome(apiSuccess, true);
        } catch (Exception e) {
            log.error("【{}】入职状态行回写异常, employeeNo={}, syncStatus={}",
                    BIZ, item.getEmployeeNo(), syncStatus, e);
            return new ItemOutcome(apiSuccess, false);
        }
    }

    /**
     * 回写离职同步结果到状态表
     *
     * @param item          待处理记录
     * @param apiSuccess    Moka resign 调用是否成功
     * @param syncStatus    离职同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param error         结果摘要（允许为空）
     * @param syncTime      同步时间
     * @param applicationId Moka 申请 id（来自 candidate_base JOIN）
     * @return 单条处理结果（dbWriteOk 反映回写是否成功）
     */
    private ItemOutcome writeResignStatus(MokaPersonStatusDO item, boolean apiSuccess, int syncStatus,
                                          String error, LocalDateTime syncTime, Integer applicationId) {
        try {
            int rows = mokaPersonStatusService.upsertResignResult(item, syncStatus, error, syncTime, applicationId);
            if (rows == 0) {
                log.info("【{}】离职状态行回写影响 0 行, employeeNo={}, syncStatus={}",
                        BIZ, item.getEmployeeNo(), syncStatus);
                return new ItemOutcome(false, false);
            }
            return new ItemOutcome(apiSuccess, true);
        } catch (Exception e) {
            log.error("【{}】离职状态行回写异常, employeeNo={}, syncStatus={}",
                    BIZ, item.getEmployeeNo(), syncStatus, e);
            return new ItemOutcome(apiSuccess, false);
        }
    }

    /**
     * 截断失败原因摘要至状态表列长度上限
     *
     * @param text 原始文本（允许为空）
     * @return 长度不超过 {@link #MAX_ERROR_LENGTH} 的文本；入参为 null 时返回 null
     */
    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * 单条处理结果 —— 内部传递 Moka 调用结果、DB 回写结果
     *
     * @param apiSuccess Moka hired / resign 调用是否成功
     * @param dbWriteOk  状态行回写是否成功
     */
    private record ItemOutcome(boolean apiSuccess, boolean dbWriteOk) {
    }
}
