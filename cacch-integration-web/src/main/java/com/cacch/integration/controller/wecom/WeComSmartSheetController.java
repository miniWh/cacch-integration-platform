package com.cacch.integration.controller.wecom;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.wecom.SmartSheetConverter;
import com.cacch.integration.dto.wecom.request.UpdateSmartRecordsRequest;
import com.cacch.integration.dto.wecom.vo.SmartFieldListVO;
import com.cacch.integration.dto.wecom.vo.SmartRecordListVO;
import com.cacch.integration.dto.wecom.vo.SmartSheetVO;
import com.cacch.integration.manager.wecom.api.IWeComSmartSheetManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 企业微信智能表格查询接口
 *
 * <p>鉴权使用配置文件 {@code wecom.apps} 中自建应用的 corpid + secret，调用方无需传递密钥。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/wecom/smartsheets")
@RequiredArgsConstructor
public class WeComSmartSheetController {

    private final IWeComSmartSheetManager weComSmartSheetManager;
    private final SmartSheetConverter smartSheetConverter;

    /**
     * 查询智能表格子表列表
     *
     * @param docId            文档 docid，不可为空
     * @param sheetId          可选，指定子表 ID
     * @param needAllTypeSheet 可选，是否返回全部类型子表（含仪表盘、说明页）
     * @return 子表视图列表；无子表时返回空列表
     */
    @GetMapping("/{docId}/sheets")
    public Result<List<SmartSheetVO>> getSheets(
            @PathVariable @NotBlank String docId,
            @RequestParam(required = false) String sheetId,
            @RequestParam(required = false) Boolean needAllTypeSheet) {
        return Result.success(smartSheetConverter.toSheetVOList(
                weComSmartSheetManager.getSheets(docId, sheetId, needAllTypeSheet)));
    }

    /**
     * 查询智能表格字段列表
     *
     * @param docId   文档 docid，不可为空
     * @param sheetId 子表 ID，不可为空
     * @param offset  偏移量，默认 0
     * @param limit   分页大小，默认 100
     * @return 字段列表视图
     */
    @GetMapping("/{docId}/sheets/{sheetId}/fields")
    public Result<SmartFieldListVO> getFields(
            @PathVariable @NotBlank String docId,
            @PathVariable @NotBlank String sheetId,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        return Result.success(smartSheetConverter.toFieldListVO(
                weComSmartSheetManager.getFields(docId, sheetId, offset, limit)));
    }

    /**
     * 查询智能表格记录列表
     *
     * @param docId   文档 docid，不可为空
     * @param sheetId 子表 ID，不可为空
     * @param offset  偏移量，默认 0
     * @param limit   分页大小，默认 100
     * @return 记录列表视图；无记录时 records 为空集合
     */
    @GetMapping("/{docId}/sheets/{sheetId}/records")
    public Result<SmartRecordListVO> getRecords(
            @PathVariable @NotBlank String docId,
            @PathVariable @NotBlank String sheetId,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        return Result.success(smartSheetConverter.toRecordListVO(
                weComSmartSheetManager.getRecords(docId, sheetId, offset, limit)));
    }

    /**
     * 更新智能表格记录（回写会议状态、总控表 doc_id 等）
     *
     * @param docId   文档 docid，不可为空
     * @param sheetId 子表 ID，不可为空
     * @param request 更新记录请求体，不可为空
     * @return 更新后的记录列表视图
     */
    @PostMapping("/{docId}/sheets/{sheetId}/records/update")
    public Result<SmartRecordListVO> updateRecords(
            @PathVariable @NotBlank String docId,
            @PathVariable @NotBlank String sheetId,
            @Valid @RequestBody UpdateSmartRecordsRequest request) {
        return Result.success(smartSheetConverter.toRecordListVO(
                weComSmartSheetManager.updateRecords(docId, sheetId, request.toWriteItems())));
    }
}
