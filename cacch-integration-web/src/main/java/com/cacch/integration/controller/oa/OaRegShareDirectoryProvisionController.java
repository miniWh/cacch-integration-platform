package com.cacch.integration.controller.oa;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.common.dto.oa.OaRegShareDirProvisionResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.oa.OaRegShareDirProvisionConverter;
import com.cacch.integration.dto.oa.vo.OaRegShareDirProvisionRecordVO;
import com.cacch.integration.entity.oa.OaRegShareDirProvisionDO;
import com.cacch.integration.manager.oa.api.IOaRegShareDirectoryProvisionManager;
import com.cacch.integration.service.oa.api.IOaRegShareDirProvisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 共享盘目录治理接口（REQ-OA-002）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/oa/reg-reports/share-directory/provision")
@RequiredArgsConstructor
public class OaRegShareDirectoryProvisionController {

    private static final String BIZ = "OaShareDirProvision";

    private final IOaRegShareDirectoryProvisionManager provisionManager;
    private final IOaRegShareDirProvisionService provisionService;
    private final OaRegShareDirProvisionConverter provisionConverter;

    /**
     * 手动触发目录治理
     *
     * @param formMainId 可选 OA 主表 ID；不传则按游标全量分批
     * @return 本轮治理统计
     */
    @PostMapping("/trigger")
    public Result<OaRegShareDirProvisionResult> trigger(
            @RequestParam(required = false) Long formMainId) {
        log.info("【{}】手动触发目录治理, formMainId={}", BIZ, formMainId);
        return Result.success(provisionManager.provisionDirectories(formMainId));
    }

    /**
     * 分页查询治理记录（可选过滤）
     *
     * @param runId     执行轮次标识，可空
     * @param ownerName 登记负责人，可空；非空时模糊匹配
     * @param action    治理动作，可空；非空时精确匹配
     * @param page      页码，从 1 开始，默认 1
     * @param size      每页条数，默认 20
     * @return 分页治理记录
     */
    @GetMapping("/records")
    public Result<Map<String, Object>> pageRecords(
            @RequestParam(required = false) String runId,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        log.info("【{}】查询治理记录, runId={}, ownerName={}, action={}, page={}, size={}",
                BIZ, runId, ownerName, action, page, size);
        IPage<OaRegShareDirProvisionDO> result = provisionService.pageQuery(runId, ownerName, action, page, size);
        List<OaRegShareDirProvisionRecordVO> records = provisionConverter.toVOList(result.getRecords());
        log.info("【{}】查询治理记录完成, total={}, page={}, size={}",
                BIZ, result.getTotal(), result.getCurrent(), result.getSize());
        return Result.success(pagePayload(result, records));
    }

    private Map<String, Object> pagePayload(IPage<OaRegShareDirProvisionDO> page,
                                            List<OaRegShareDirProvisionRecordVO> records) {
        Map<String, Object> payload = new HashMap<>(8);
        payload.put("records", records);
        payload.put("total", page.getTotal());
        payload.put("page", page.getCurrent());
        payload.put("size", page.getSize());
        payload.put("pages", page.getPages());
        return payload;
    }
}
