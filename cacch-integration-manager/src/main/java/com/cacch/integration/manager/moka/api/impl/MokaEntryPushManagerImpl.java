package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.common.enums.moka.MokaEntrySyncStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.moka.MokaEntryPushItemVO;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateResponse;
import com.cacch.integration.manager.moka.api.IMokaEntryPushManager;
import com.cacch.integration.mapper.moka.MokaEntryApplicationMapper;
import com.cacch.integration.mapper.moka.MokaEntrySyncMapper;
import com.cacch.integration.service.ihr.api.IIhrStaffEntryFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Moka 待入职 → iHR 待入职 推送编排实现
 *
 * <p>流程：PG 联合查询 4 表 + ihr_department JOIN → 逐条前置校验 →
 * 字段映射 → 调 iHR 新增待入职接口 → 更新同步状态。</p>
 *
 * <p>事务策略：Manager 层<strong>不加</strong> @Transactional。
 * <ul>
 *     <li>iHR 是 HTTP 调用，禁止在事务内执行</li>
 *     <li>每条推送完独立 UPDATE 同步状态，失败不阻断批次</li>
 *     <li>下次重跑时 PENDING/FAILED 记录会再次进入推送队列</li>
 * </ul></p>
 *
 * <p>与 {@link MokaPersonPushManagerImpl}（批量推 Moka）不同：
 * 本 Manager 逐条推送 iHR（iHR 无批量接口），每条独立 try-catch。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaEntryPushManagerImpl implements IMokaEntryPushManager {

    private static final String BIZ = "Moka待入职推送iHR";

    /**
     * 默认推送状态：PENDING + FAILED（SKIPPED 的记录用户可能已修复，重新纳入）
     */
    private static final List<String> DEFAULT_STATUSES = List.of(
            MokaEntrySyncStatusEnum.PENDING.getCode(),
            MokaEntrySyncStatusEnum.FAILED.getCode(),
            MokaEntrySyncStatusEnum.SKIPPED.getCode()
    );

    /**
     * iHR 入职日期格式
     */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MokaEntryApplicationMapper applicationMapper;
    private final MokaEntrySyncMapper syncMapper;
    private final IIhrStaffEntryFormService ihrEntryFormService;

    @Override
    public MokaEntryPushSummary pushToIhr(List<String> statusList, int limit) {
        log.info("【{}】开始执行推送, statusList={}, limit={}", BIZ, statusList, limit);

        // —— 1. 读取待推送记录 ——
        List<String> actualStatuses = (statusList == null || statusList.isEmpty())
                ? DEFAULT_STATUSES : statusList;

        List<MokaEntryPushItemVO> items = applicationMapper.listForPush(actualStatuses, limit);
        int totalRead = items == null ? 0 : items.size();
        log.info("【{}】读取待推送记录完成, totalRead={}, statusList={}", BIZ, totalRead, actualStatuses);

        if (totalRead == 0) {
            log.info("【{}】无待推送记录, 推送结束", BIZ);
            return new MokaEntryPushSummary(0, 0, 0, 0);
        }

        // —— 2. 逐条推送 ——
        MokaEntryPushSummary summary = new MokaEntryPushSummary(0, 0, 0, 0);

        for (MokaEntryPushItemVO item : items) {
            summary = processOne(item, summary);
        }

        log.info("【{}】推送完成, total={}, success={}, failed={}, skipped={}",
                BIZ, summary.total(), summary.success(), summary.failed(), summary.skipped());

        return summary;
    }

    /**
     * 处理单条候选人推送（前置校验 → iHR 调用 → 状态更新）
     */
    private MokaEntryPushSummary processOne(MokaEntryPushItemVO item, MokaEntryPushSummary summary) {
        Integer appId = item.getApplicationId();

        // —— 2a. 前置校验 ——
        String skipReason = validatePushItem(item);
        if (skipReason != null) {
            log.info("【{}】跳过推送, applicationId={}, reason={}", BIZ, appId, skipReason);
            updateSyncResult(appId, MokaEntrySyncStatusEnum.SKIPPED, null, skipReason);
            return summary.add("skipped");
        }

        // —— 2b. 构建 iHR 请求体 ——
        IhrStaffEntryFormCreateRequest request = buildIhrRequest(item);

        // —— 2c. 调 iHR ——
        try {
            IhrStaffEntryFormCreateResponse response = ihrEntryFormService.createEntryForm(request);

            if (response != null && response.isSuccess()) {
                String entryId = response.getEntryId();
                log.info("【{}】推送成功, applicationId={}, ihrEntryId={}", BIZ, appId, entryId);
                updateSyncResult(appId, MokaEntrySyncStatusEnum.SUCCESS, entryId, "推送成功");
                return summary.add("success");
            } else {
                String errMsg = response == null ? "空响应"
                        : ("code=" + response.getCode() + ",message=" + response.getMessage());
                log.info("【{}】推送终止(业务失败), applicationId={}, {}", BIZ, appId, errMsg);
                updateSyncResult(appId, MokaEntrySyncStatusEnum.FAILED, null, errMsg);
                return summary.add("failed");
            }
        } catch (BizException e) {
            log.info("【{}】推送终止(BizException), applicationId={}, reason={}", BIZ, appId, e.getMessage());
            updateSyncResult(appId, MokaEntrySyncStatusEnum.FAILED, null, e.getMessage());
            return summary.add("failed");
        } catch (Exception e) {
            log.info("【{}】推送终止(异常), applicationId={}, reason={}", BIZ, appId, e.getMessage());
            log.error("【{}】iHR 新增待入职异常, applicationId={}", BIZ, appId, e);
            updateSyncResult(appId, MokaEntrySyncStatusEnum.FAILED, null, "HTTP异常: " + e.getMessage());
            return summary.add("failed");
        }
    }

    /**
     * 前置校验：不满足推送条件时返回跳过原因
     *
     * @return null 表示校验通过；非 null 为跳过原因描述
     */
    private String validatePushItem(MokaEntryPushItemVO item) {
        if (item.getName() == null || item.getName().isBlank()) {
            return "候选人姓名缺失";
        }
        if (item.getPhone() == null || item.getPhone().isBlank()) {
            return "手机号缺失";
        }
        if (item.getCheckInDate() == null) {
            return "入职日期缺失";
        }
        if (item.getIhrDeptId() == null) {
            return "iHR部门ID缺失(moka_department_code无匹配)";
        }
        return null;
    }

    /**
     * 构建 iHR 新增待入职请求体
     *
     * <p>字段映射：
     * <ul>
     *     <li>必填：staffName(name) / mobile(phone) / expectEntryDate(checkInDate) / departmentId(ihrDeptId)</li>
     *     <li>性别：1→MALE 2→FEMALE（iHR 枚举值）</li>
     *     <li>证件号：citizenId（可空），证件类型暂不映射（Moka certificateType 与 iHR 枚举不一致）</li>
     *     <li>招聘模式：hireMode 1→社招 2→校招，映射为 iHR staffType（暂传 null）</li>
     * </ul></p>
     */
    private IhrStaffEntryFormCreateRequest buildIhrRequest(MokaEntryPushItemVO item) {
        IhrStaffEntryFormCreateRequest req = new IhrStaffEntryFormCreateRequest();
        req.setStaffName(item.getName());
        req.setMobile(item.getPhone());
        req.setEmail(item.getEmail());

        // 入职日期：LocalDateTime → yyyy-MM-dd
        req.setExpectEntryDate(item.getCheckInDate().format(DATE_FMT));

        // 性别：1-男 → MALE，2-女 → FEMALE
        if (item.getGender() != null) {
            req.setSex(switch (item.getGender()) {
                case 1 -> "MALE";
                case 2 -> "FEMALE";
                default -> null;
            });
        }

        // iHR 部门 ID（通过 department_code JOIN 得到，Long 类型）
        req.setDepartmentId(item.getIhrDeptId());

        // 证件号（可空）
        req.setIdCardNo(item.getCitizenId());

        // 招聘模式映射：hireMode=1 社招 → FULLTIME，hireMode=2 校招 → INTERSHIP
        if (item.getHireMode() != null) {
            req.setStaffType(switch (item.getHireMode()) {
                case 1 -> "FULLTIME";
                case 2 -> "INTERSHIP";
                default -> null;
            });
        }

        // 备注：写入 offer 相关信息便于追溯
        StringBuilder remark = new StringBuilder();
        if (item.getMokaJobTitle() != null) {
            remark.append("职位: ").append(item.getMokaJobTitle());
        }
        if (item.getOfferDepartmentName() != null) {
            if (!remark.isEmpty()) remark.append("; ");
            remark.append("Offer部门: ").append(item.getOfferDepartmentName());
        }
        if (item.getMokaStageName() != null) {
            if (!remark.isEmpty()) remark.append("; ");
            remark.append("阶段: ").append(item.getMokaStageName());
        }
        if (!remark.isEmpty()) {
            req.setRemark(remark.toString());
        }

        return req;
    }

    /**
     * 更新单条同步结果
     */
    private void updateSyncResult(Integer applicationId, MokaEntrySyncStatusEnum status,
                                  String ihrEntryId, String message) {
        try {
            syncMapper.updateResult(applicationId, status.getCode(),
                    ihrEntryId, message, LocalDateTime.now());
        } catch (Exception e) {
            // 状态回写失败不影响推送主体结果，但必须记录
            log.info("【{}】同步状态回写终止, applicationId={}, status={}, reason={}",
                    BIZ, applicationId, status.getCode(), e.getMessage());
        }
    }
}
