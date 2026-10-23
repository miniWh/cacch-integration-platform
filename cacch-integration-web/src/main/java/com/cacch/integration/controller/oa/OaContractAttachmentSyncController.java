package com.cacch.integration.controller.oa;

import com.cacch.integration.common.dto.oa.OaContractAttachmentSyncResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.oa.request.OaContractAttachmentSyncExecuteRequest;
import com.cacch.integration.manager.oa.api.IOaContractAttachmentSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OA 货物购销合同附件回传 CRM 接口
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/oa/contract-attachments")
@RequiredArgsConstructor
public class OaContractAttachmentSyncController {

    private static final String BIZ = "OA购销合同附件回传CRM";

    private final IOaContractAttachmentSyncManager syncManager;

    /**
     * 执行一次扫描与回传（ESB 定时调用）
     *
     * <p>先按 NOT IN 增量扫描 OA 购销合同写入中间表，再处理中间表中 PENDING / RETRY 记录。
     * 若上一轮仍在执行（未获取到执行锁），立即返回 executed=false，不做排队。</p>
     *
     * @param request 执行参数，可空；不传时按增量扫描、不限定订单号
     * @return 本次执行统计；executed=false 时 message 说明原因
     */
    @PostMapping("/execute")
    public Result<OaContractAttachmentSyncResult> execute(
            @RequestBody(required = false) OaContractAttachmentSyncExecuteRequest request) {
        boolean fullScan = request != null && Boolean.TRUE.equals(request.getFullScan());
        String ncOrderNo = request == null ? null : request.getNcOrderNo();
        log.info("【{}】执行接口被调用, fullScan={}, ncOrderNo={}", BIZ, fullScan, ncOrderNo);
        return Result.success(syncManager.execute(fullScan, ncOrderNo));
    }
}
