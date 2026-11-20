package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.common.constant.moka.MokaPersonConstants;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.moka.MokaPersonDO;
import com.cacch.integration.integration.moka.client.MokaUserClient;
import com.cacch.integration.integration.moka.client.MokaUserListClient;
import com.cacch.integration.integration.moka.client.dto.MokaUserListV3Item;
import com.cacch.integration.integration.moka.client.dto.MokaUserListV3Request;
import com.cacch.integration.integration.moka.client.dto.MokaUserListV3Response;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncResponse;
import com.cacch.integration.manager.moka.api.IMokaPersonPushManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import com.cacch.integration.service.moka.api.IMokaOAuth2TokenService;
import com.cacch.integration.service.moka.api.IMokaPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Moka 人员推送编排 —— V3 全量对账 → 角色门禁 → 分批 syncInfo
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaPersonPushManagerImpl implements IMokaPersonPushManager {

    private static final String BIZ = "Moka 人员推送";

    private static final int BATCH_SIZE = 100;

    private static final List<Integer> PUSH_STATUSES = List.of(
            MokaPersonConstants.SYNC_PENDING, MokaPersonConstants.SYNC_FAILED);

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_ROLE_GATE = "role_gate_skip";

    private final IMokaPersonService mokaPersonService;
    private final MokaUserClient mokaUserClient;
    private final MokaUserListClient mokaUserListClient;
    private final IMokaOAuth2TokenService mokaOAuth2TokenService;
    private final IWeComWebhookManager weComWebhookManager;

    @Override
    public MokaPersonPushResult pushToMoka() {
        log.info("【{}】开始执行 V3 对账 + 中间表 → Moka 推送", BIZ);

        ReconcileStats reconcileStats;
        try {
            reconcileStats = reconcileAllFromMokaV3();
        } catch (BizException e) {
            alertReconcileFailed(e.getMessage());
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            alertReconcileFailed(msg);
            log.error("【{}】V3 对账异常", BIZ, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "Moka 人员 V3 对账失败: " + msg, e);
        }

        List<MokaPersonDO> candidates = mokaPersonService.listBySyncStatusIn(PUSH_STATUSES);
        int totalRead = candidates == null ? 0 : candidates.size();

        List<MokaPersonDO> pushable = new ArrayList<>();
        List<Long> gateSkipIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(candidates)) {
            for (MokaPersonDO p : candidates) {
                if (p == null) {
                    continue;
                }
                if (!MokaPersonConstants.isAutoSyncEligible(p.getMokaUserId(), p.getMokaBuiltinRole())) {
                    if (p.getId() != null) {
                        gateSkipIds.add(p.getId());
                    }
                    continue;
                }
                pushable.add(p);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        int roleGateSkipped = 0;
        if (!gateSkipIds.isEmpty()) {
            roleGateSkipped = mokaPersonService.batchUpdateSyncStatus(
                    gateSkipIds, MokaPersonConstants.SYNC_SKIP, now, RESULT_ROLE_GATE);
        }

        int pushTotal = pushable.size();
        if (pushTotal == 0) {
            log.info("【{}】无待推送记录, reconcileFetched={}, roleGateSkipped={}",
                    BIZ, reconcileStats.fetched(), roleGateSkipped);
            return new MokaPersonPushResult(reconcileStats.fetched(), reconcileStats.updated(), roleGateSkipped,
                    totalRead, 0, true, 0, 0, 0);
        }

        int batchCount = (pushTotal + BATCH_SIZE - 1) / BATCH_SIZE;
        boolean allApiSuccess = true;
        int syncedCount = 0;
        int syncFailedCount = 0;
        int dbUpdateFailedCount = 0;

        for (int i = 0; i < batchCount; i++) {
            int from = i * BATCH_SIZE;
            int to = Math.min(from + BATCH_SIZE, pushTotal);
            List<MokaPersonDO> batch = pushable.subList(from, to);

            MokaUserSyncRequest request = buildSyncRequest(batch);
            PushOutcome outcome = pushOneBatch(batch, request);

            List<Long> ids = new ArrayList<>(batch.size());
            for (MokaPersonDO p : batch) {
                if (p != null && p.getId() != null) {
                    ids.add(p.getId());
                }
            }

            int updated = mokaPersonService.batchUpdateSyncStatus(
                    ids, outcome.status(), now, outcome.result());

            dbUpdateFailedCount += ids.size() - updated;
            if (outcome.status() == MokaPersonConstants.SYNC_SYNCED) {
                syncedCount += updated;
            } else {
                syncFailedCount += updated;
            }
            if (!outcome.apiSuccess()) {
                allApiSuccess = false;
            }
        }

        log.info("【{}】推送完成, reconcileFetched={}, reconcileUpdated={}, roleGateSkipped={}, pushable={}, synced={}, failed={}",
                BIZ, reconcileStats.fetched(), reconcileStats.updated(), roleGateSkipped,
                pushTotal, syncedCount, syncFailedCount);

        return new MokaPersonPushResult(reconcileStats.fetched(), reconcileStats.updated(), roleGateSkipped,
                totalRead, batchCount, allApiSuccess, syncedCount, syncFailedCount, dbUpdateFailedCount);
    }

    private ReconcileStats reconcileAllFromMokaV3() {
        String accessToken = mokaOAuth2TokenService.getAccessToken();
        List<MokaUserListV3Item> allUsers = new ArrayList<>();
        Long nextUserId = null;
        int page = 0;

        do {
            page++;
            MokaUserListV3Request request = new MokaUserListV3Request();
            request.setLimit(String.valueOf(MokaPersonConstants.USER_LIST_V3_PAGE_LIMIT));
            request.setOrder("DESC");
            request.setNextUserId(nextUserId);

            MokaUserListV3Response response;
            try {
                response = mokaUserListClient.listUsers(request, accessToken);
            } catch (RestClientException e) {
                log.info("【{}】V3 对账第 {} 页 HTTP 失败, reason={}", BIZ, page, e.getMessage());
                throw new BizException(ResultCode.INTEGRATION_ERROR, "Moka 人事 V3 HTTP 调用失败: " + e.getMessage(), e);
            }

            if (response == null || !response.isSuccess() || response.getData() == null) {
                String detail = response == null ? "response is null"
                        : "code=" + response.getCode() + ",msg=" + response.getMsg();
                log.info("【{}】V3 对账第 {} 页业务失败, {}", BIZ, page, detail);
                throw new BizException(ResultCode.INTEGRATION_ERROR, "Moka 人事 V3 对账失败: " + detail);
            }

            List<MokaUserListV3Item> pageList = response.getData().getUserList();
            if (!CollectionUtils.isEmpty(pageList)) {
                allUsers.addAll(pageList);
            }
            nextUserId = response.getData().getNextUserId();
        } while (nextUserId != null);

        List<IMokaPersonService.MokaPlatformUserSnapshot> snapshots = new ArrayList<>(allUsers.size());
        for (MokaUserListV3Item item : allUsers) {
            if (item == null || !StringUtils.hasText(item.getPhone())) {
                continue;
            }
            snapshots.add(new IMokaPersonService.MokaPlatformUserSnapshot(
                    item.getPhone(), item.getUserId(), item.getRoleId(), item.getRole()));
        }

        int updated = mokaPersonService.reconcilePlatformSnapshots(snapshots, LocalDateTime.now());
        log.info("【{}】V3 对账完成, fetched={}, matchedSnapshots={}, localUpdated={}",
                BIZ, allUsers.size(), snapshots.size(), updated);
        return new ReconcileStats(allUsers.size(), updated);
    }

    private void alertReconcileFailed(String errorMessage) {
        String hourKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                .biz("moka")
                .title("Moka 人员推送对账失败")
                .subject("pushToMoka")
                .context("对账失败已终止推送，请检查 OAuth2 与 V3 接口")
                .errorMessage(errorMessage)
                .dedupType("moka-person-reconcile")
                .dedupId(hourKey)
                .mention(true)
                .build());
    }

    private MokaUserSyncRequest buildSyncRequest(List<MokaPersonDO> batch) {
        List<MokaUserSyncRequest.MokaUserInfo> usersInfo = new ArrayList<>(batch.size());
        for (MokaPersonDO p : batch) {
            if (p == null) {
                log.info("【{}】构建请求体跳过 null DO", BIZ);
                continue;
            }
            boolean firstCreate = p.getMokaUserId() == null;

            MokaUserSyncRequest.MokaUserInfo info = new MokaUserSyncRequest.MokaUserInfo();
            info.setPhone(p.getContactPhone());
            info.setName(p.getUserName());
            info.setNickname(p.getNickname());
            info.setEmail(p.getCompanyEmail());
            info.setNumber(p.getEmployeeNo());
            if (firstCreate) {
                info.setRoleId(MokaConstants.DEFAULT_ROLE_ID);
            }
            String deptCode = p.getDepartmentCode();
            if (StringUtils.hasText(deptCode)) {
                info.setDepartmentCode(new String[]{deptCode});
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

    private PushOutcome pushOneBatch(List<MokaPersonDO> batch, MokaUserSyncRequest request) {
        try {
            MokaUserSyncResponse response = mokaUserClient.syncUserInfo(request);
            if (response == null) {
                return new PushOutcome(MokaPersonConstants.SYNC_FAILED, "response is null", false);
            }
            if (response.isSuccess()) {
                return new PushOutcome(MokaPersonConstants.SYNC_SYNCED, RESULT_SUCCESS, true);
            }
            String result = "code=" + response.getCode() + ",msg=" + response.getMsg();
            return new PushOutcome(MokaPersonConstants.SYNC_FAILED, result, false);
        } catch (RestClientException e) {
            String result = "httpError=" + e.getMessage();
            log.error("【{}】Moka syncInfo HTTP 异常", BIZ, e);
            return new PushOutcome(MokaPersonConstants.SYNC_FAILED, result, false);
        }
    }

    private record ReconcileStats(int fetched, int updated) {
    }

    private record PushOutcome(int status, String result, boolean apiSuccess) {
    }
}