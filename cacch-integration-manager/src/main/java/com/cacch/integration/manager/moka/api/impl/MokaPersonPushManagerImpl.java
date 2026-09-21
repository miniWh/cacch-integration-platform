package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.entity.moka.MokaPersonDO;
import com.cacch.integration.integration.moka.client.MokaUserClient;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncResponse;
import com.cacch.integration.manager.moka.api.IMokaPersonPushManager;
import com.cacch.integration.service.moka.api.IMokaPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Moka 人员推送编排实现 —— 中间表分批读取 → Moka API 推送 → 状态回写
 *
 * <p>调用链：Controller → pushToMoka() → IMokaPersonService.listBySyncStatusIn()
 * （查 PENDING+SYNC_FAILED）→ 内存分批（≤ 100 条 / 批）→
 * MokaUserClient.syncUserInfo()（HTTP）→
 * IMokaPersonService.batchUpdateSyncStatus()（DB 回写）。</p>
 *
 * <p>事务策略：Manager 层不在外层包裹 {@code @Transactional}。
 * <ul>
 *     <li>HTTP 调用 {@link MokaUserClient#syncUserInfo} 不涉及 DB 操作，禁止加事务</li>
 *     <li>DB 状态回写由 {@link IMokaPersonService#batchUpdateSyncStatus}
 *     内部显式声明事务（PROPAGATION.REQUIRED, timeout=120s）</li>
 *     <li>每批的 HTTP 与 DB 回写之间无强一致性约束：HTTP 成功后 DB 回写失败，
 *     记入 {@code dbUpdateFailedCount}；下次重跑时会再次推送（sync_status 仍为 0）</li>
 * </ul>
 * </p>
 *
 * <p>分批策略：Moka syncInfo API 单次推送上限 100 条，
 * 使用 {@code subList(fromIndex, toIndex)} 切片，每批独立调用 Moka API。</p>
 *
 * <p>失败处理：
 * <ul>
 *     <li>Moka API 返回失败（code 非 0/200）→ 整批回写 SYNC_FAILED，记 msg 摘要</li>
 *     <li>HTTP 调用抛 RestClientException → 整批回写 SYNC_FAILED，记异常 message</li>
 *     <li>DB 回写失败（batchUpdateSyncStatus 返回值小于批次大小）→ 计入 dbUpdateFailedCount</li>
 * </ul>
 * </p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaPersonPushManagerImpl implements IMokaPersonPushManager {

    private static final String BIZ = "Moka 人员推送";

    /**
     * Moka syncInfo API 单次推送上限 —— 超过此值需要分批
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 推送范围：PENDING + SYNC_FAILED，避免重复推送已 SYNCED 记录；
     * 叠加 deactivated=0（在职）+ is_deleted=0（@TableLogic 自动过滤）
     */
    private static final List<Integer> PUSH_STATUSES = List.of(0, 2);

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

    private final IMokaPersonService mokaPersonService;
    private final MokaUserClient mokaUserClient;

    @Override
    public MokaPersonPushResult pushToMoka() {
        log.info("【{}】开始执行中间表 → Moka 开放平台推送", BIZ);

        // —— 1. 读取待推送记录 ——
        List<MokaPersonDO> persons = mokaPersonService.listBySyncStatusIn(PUSH_STATUSES);
        int totalRead = persons == null ? 0 : persons.size();
        log.info("【{}】读取待推送记录完成, totalRead={}, syncStatuses={}",
                BIZ, totalRead, PUSH_STATUSES);

        if (totalRead == 0) {
            log.info("【{}】无待推送记录, 推送结束", BIZ);
            return new MokaPersonPushResult(0, 0, true, 0, 0, 0);
        }

        // —— 2. 分批推送 ——
        int batchCount = (totalRead + BATCH_SIZE - 1) / BATCH_SIZE;
        boolean allApiSuccess = true;
        int syncedCount = 0;
        int syncFailedCount = 0;
        int dbUpdateFailedCount = 0;

        for (int i = 0; i < batchCount; i++) {
            int from = i * BATCH_SIZE;
            int to = Math.min(from + BATCH_SIZE, totalRead);
            List<MokaPersonDO> batch = persons.subList(from, to);

            log.info("【{}】开始推送第 {}/{} 批, batchSize={}, fromIndex={}, toIndex={}",
                    BIZ, i + 1, batchCount, batch.size(), from, to);

            // —— 2.1 DO → Moka API DTO 转换 ——
            MokaUserSyncRequest request = buildSyncRequest(batch);

            // —— 2.2 调用 Moka API（HTTP 不包事务）——
            PushOutcome outcome = pushOneBatch(batch, request);

            // —— 2.3 回写 DB 状态（独立事务）——
            List<Long> ids = new ArrayList<>(batch.size());
            for (MokaPersonDO p : batch) {
                if (p != null && p.getId() != null) {
                    ids.add(p.getId());
                }
            }

            LocalDateTime syncTime = LocalDateTime.now();
            int updated = mokaPersonService.batchUpdateSyncStatus(
                    ids, outcome.status(), syncTime, outcome.result());

            int dbFailed = ids.size() - updated;
            dbUpdateFailedCount += dbFailed;

            if (outcome.status() == STATUS_SYNCED) {
                syncedCount += updated;
            } else {
                syncFailedCount += updated;
            }

            if (!outcome.apiSuccess()) {
                allApiSuccess = false;
            }

            log.info("【{}】第 {}/{} 批完成, apiSuccess={}, pushStatus={}, dbUpdated={}, dbFailed={}",
                    BIZ, i + 1, batchCount, outcome.apiSuccess(),
                    outcome.status() == STATUS_SYNCED ? "SYNCED" : "SYNC_FAILED",
                    updated, dbFailed);
        }

        log.info("【{}】推送完成, totalRead={}, batchCount={}, mokaApiSuccess={}, syncedCount={}, syncFailedCount={}, dbUpdateFailedCount={}",
                BIZ, totalRead, batchCount, allApiSuccess,
                syncedCount, syncFailedCount, dbUpdateFailedCount);

        return new MokaPersonPushResult(totalRead, batchCount, allApiSuccess,
                syncedCount, syncFailedCount, dbUpdateFailedCount);
    }

    /**
     * 把一批 MokaPersonDO 转换为 Moka syncInfo 请求体
     *
     * <p>固定参数取自 {@link MokaConstants}：
     * {@code uniqueType / autoActivated / updateDepartment /
     * updateSuperiorEmail / thirdPartyId / locale / timezone}。</p>
     *
     * <p>防御性处理：跳过 null DO；contactPhone 为空时仍构建请求项
     * （由 Moka API 自行拒绝，便于追溯问题记录）。</p>
     *
     * @param batch 一批 DO（≤ 100 条）
     * @return syncInfo 请求体
     */
    private MokaUserSyncRequest buildSyncRequest(List<MokaPersonDO> batch) {
        List<MokaUserSyncRequest.MokaUserInfo> usersInfo = new ArrayList<>(batch.size());
        for (MokaPersonDO p : batch) {
            if (p == null) {
                log.info("【{}】构建请求体跳过 null DO", BIZ);
                continue;
            }
            MokaUserSyncRequest.MokaUserInfo info = new MokaUserSyncRequest.MokaUserInfo();
            info.setPhone(p.getContactPhone());
            info.setName(p.getUserName());
            info.setNickname(p.getNickname());
            info.setEmail(p.getCompanyEmail());
            info.setNumber(p.getEmployeeNo());
            info.setRoleId(p.getRoleId() != null ? p.getRoleId() : MokaConstants.DEFAULT_ROLE_ID);
            // departmentCode Moka API 要求数组形式
            String deptCode = p.getDepartmentCode();
            if (StringUtils.hasText(deptCode)) {
                info.setDepartmentCode(new String[]{deptCode});
            } else {
                info.setDepartmentCode(new String[0]);
            }
            info.setDeactivated(p.getDeactivated() != null ? p.getDeactivated() : 0);
            info.setUniqueType(MokaConstants.USER_UNIQUE_TYPE);
            info.setAutoActivated(MokaConstants.USER_AUTO_ACTIVATED);
            info.setUpdateDepartment(MokaConstants.USER_UPDATE_DEPARTMENT);
            info.setUpdateSuperiorEmail(MokaConstants.USER_UPDATE_SUPERIOR_EMAIL);
            info.setThirdPartyId(MokaConstants.USER_THIRD_PARTY_ID);
            info.setLocale(StringUtils.hasText(p.getLocale()) ? p.getLocale() : MokaConstants.USER_LOCALE);
            info.setTimezone(StringUtils.hasText(p.getTimezone()) ? p.getTimezone() : MokaConstants.USER_TIMEZONE);
            usersInfo.add(info);
        }
        MokaUserSyncRequest request = new MokaUserSyncRequest();
        request.setUsersInfo(usersInfo);
        return request;
    }

    /**
     * 推送单批到 Moka API —— 区分 API 业务失败与 HTTP 异常
     *
     * @param batch   当前批 DO（用于日志追溯）
     * @param request 已构建的请求体
     * @return 推送结果（status=1=SYNCED / 2=SYNC_FAILED + result 摘要 + apiSuccess）
     */
    private PushOutcome pushOneBatch(List<MokaPersonDO> batch, MokaUserSyncRequest request) {
        try {
            MokaUserSyncResponse response = mokaUserClient.syncUserInfo(request);
            if (response == null) {
                log.info("【{}】Moka API 返回空响应, 标记整批 SYNC_FAILED, batchSize={}",
                        BIZ, batch.size());
                return new PushOutcome(STATUS_SYNC_FAILED, "response is null", false);
            }
            if (response.isSuccess()) {
                return new PushOutcome(STATUS_SYNCED, RESULT_SUCCESS, true);
            }
            String result = "code=" + response.getCode() + ",msg=" + response.getMsg();
            log.info("【{}】Moka API 业务失败, 标记整批 SYNC_FAILED, batchSize={}, {}",
                    BIZ, batch.size(), result);
            return new PushOutcome(STATUS_SYNC_FAILED, result, false);
        } catch (RestClientException e) {
            String result = "httpError=" + e.getMessage();
            log.info("【{}】Moka HTTP 调用失败, 标记整批 SYNC_FAILED, batchSize={}, reason={}",
                    BIZ, batch.size(), e.getMessage());
            log.error("【{}】Moka syncInfo HTTP 调用异常", BIZ, e);
            return new PushOutcome(STATUS_SYNC_FAILED, result, false);
        }
    }

    /**
     * 单批推送结果 —— 内部传递状态、摘要、API 是否成功
     */
    private record PushOutcome(int status, String result, boolean apiSuccess) {
    }
}
