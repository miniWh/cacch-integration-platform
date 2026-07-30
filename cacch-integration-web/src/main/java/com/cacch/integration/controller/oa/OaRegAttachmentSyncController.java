package com.cacch.integration.controller.oa;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.common.dto.oa.OaRegAttachmentSyncResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.convert.oa.OaRegAttachmentSyncConverter;
import com.cacch.integration.dto.oa.vo.OaRegAttachmentSyncRecordVO;
import com.cacch.integration.entity.oa.OaRegAttachmentSyncDO;
import com.cacch.integration.manager.oa.api.IOaRegAttachmentSyncManager;
import com.cacch.integration.service.oa.api.IOaRegAttachmentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 国内登记报告资料列表附件同步接口
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/oa/reg-reports")
@RequiredArgsConstructor
public class OaRegAttachmentSyncController {

    private static final String BIZ = "OaRegAttachmentSync";

    private final IOaRegAttachmentSyncManager syncManager;
    private final IOaRegAttachmentSyncService syncService;
    private final OaRegAttachmentSyncConverter syncConverter;

    /**
     * 手动触发附件同步
     *
     * @param formMainId 可选 OA 主表 ID；不传则按 batch-size 扫描
     * @param formBizNo  可选表单业务编号（如 REG-202607-0128）；暂不支持，请使用 formMainId
     * @return 本轮同步统计
     */
    @PostMapping("/attachment-sync/trigger")
    public Result<OaRegAttachmentSyncResult> trigger(
            @RequestParam(required = false) Long formMainId,
            @RequestParam(required = false) String formBizNo) {
        if (formMainId == null && StringUtils.hasText(formBizNo)) {
            log.info("【{}】触发同步终止, reason=formBizNo暂不支持, formBizNo={}", BIZ, formBizNo.trim());
            throw new BizException(ResultCode.PARAM_INVALID,
                    "formBizNo 查询暂未实现，请传 formMainId（OA 主表 ID）");
        }
        return Result.success(syncManager.syncAttachments(formMainId));
    }

    /**
     * 按表单标识查询同步记录（REQ-OA-001 路径）
     *
     * @param bizNo 表单标识；当前仅支持 numeric 形式的 formMainId
     * @param page  页码，从 1 开始，默认 1
     * @param size  每页条数，默认 20
     * @return 分页同步记录
     */
    @GetMapping("/{bizNo}/attachment-sync/records")
    public Result<Map<String, Object>> listByBizNo(@PathVariable String bizNo,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size) {
        Long formMainId = resolveFormMainId(bizNo);
        IPage<OaRegAttachmentSyncDO> result = syncService.pageByFormMainId(formMainId, page, size);
        List<OaRegAttachmentSyncRecordVO> records = syncConverter.toVOList(result.getRecords());
        return Result.success(pagePayload(result, records));
    }

    /**
     * 按 OA 主表 ID 查询同步记录（兼容路径）
     *
     * @param formMainId OA 主表 formmain_4070.id
     * @param page       页码，从 1 开始，默认 1
     * @param size       每页条数，默认 20
     * @return 分页同步记录
     */
    @GetMapping("/attachment-sync/records/by-form/{formMainId}")
    public Result<Map<String, Object>> listByFormMainId(@PathVariable Long formMainId,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long size) {
        IPage<OaRegAttachmentSyncDO> result = syncService.pageByFormMainId(formMainId, page, size);
        List<OaRegAttachmentSyncRecordVO> records = syncConverter.toVOList(result.getRecords());
        return Result.success(pagePayload(result, records));
    }

    /**
     * 按登记负责人 / IPDP 名称 / 资料项目分页查询同步记录（管理端）
     *
     * <p>三个业务维度均可选、可组合；均不填时返回全部记录（分页）。名称类条件为模糊匹配（包含即可）。</p>
     *
     * @param ownerName  登记负责人，可空
     * @param ipdpName   IPDP 名称，可空
     * @param itemName   资料项目名称，可空
     * @param syncStatus 同步状态，可空；取值见 {@code OaRegAttachmentSyncStatusEnum}
     * @param page       页码，从 1 开始，默认 1
     * @param size       每页条数，默认 20，最大 100
     * @return 分页同步记录
     */
    @GetMapping("/attachment-sync/records/search")
    public Result<Map<String, Object>> searchRecords(@RequestParam(required = false) String ownerName,
                                                     @RequestParam(required = false) String ipdpName,
                                                     @RequestParam(required = false) String itemName,
                                                     @RequestParam(required = false) String syncStatus,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "20") long size) {
        log.info("【{}】管理查询同步记录, ownerName={}, ipdpName={}, itemName={}, syncStatus={}, page={}, size={}",
                BIZ, ownerName, ipdpName, itemName, syncStatus, page, size);
        IPage<OaRegAttachmentSyncDO> result = syncService.pageByItemCriteria(
                ownerName, ipdpName, itemName, syncStatus, page, size);
        List<OaRegAttachmentSyncRecordVO> records = syncConverter.toVOList(result.getRecords());
        log.info("【{}】管理查询同步记录完成, total={}, page={}, size={}",
                BIZ, result.getTotal(), result.getCurrent(), result.getSize());
        return Result.success(pagePayload(result, records));
    }

    /**
     * 分页查询同步记录
     *
     * @param syncStatus 可选同步状态过滤
     * @param page       页码，从 1 开始，默认 1
     * @param size       每页条数，默认 20
     * @return 分页同步记录
     */
    @GetMapping("/attachment-sync/records")
    public Result<Map<String, Object>> pageRecords(@RequestParam(required = false) String syncStatus,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size) {
        IPage<OaRegAttachmentSyncDO> result = syncService.pageQuery(syncStatus, page, size);
        List<OaRegAttachmentSyncRecordVO> records = syncConverter.toVOList(result.getRecords());
        return Result.success(pagePayload(result, records));
    }

    private Long resolveFormMainId(String bizNo) {
        if (!StringUtils.hasText(bizNo)) {
            throw new BizException(ResultCode.PARAM_MISSING, "bizNo 不能为空");
        }
        String trimmed = bizNo.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            log.info("【{}】解析 bizNo 终止, reason=非 numeric formMainId, bizNo={}", BIZ, trimmed);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "bizNo 暂仅支持 numeric 形式的 formMainId，表单编号查询待实现");
        }
    }

    private Map<String, Object> pagePayload(IPage<OaRegAttachmentSyncDO> page,
                                            List<OaRegAttachmentSyncRecordVO> records) {
        Map<String, Object> payload = new HashMap<>(8);
        payload.put("records", records);
        payload.put("total", page.getTotal());
        payload.put("page", page.getCurrent());
        payload.put("size", page.getSize());
        payload.put("pages", page.getPages());
        return payload;
    }
}
