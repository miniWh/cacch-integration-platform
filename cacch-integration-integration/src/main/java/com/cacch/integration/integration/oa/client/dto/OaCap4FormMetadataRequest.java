package com.cacch.integration.integration.oa.client.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CAP4 form/soap/export 请求体组装（用于获取表单元数据 definition）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaCap4FormMetadataRequest {

    private OaCap4FormMetadataRequest() {
    }

    /**
     * 组装 export 请求体（官方文档：响应 {@code data.data.definition} 含字段定义）
     *
     * @param templateCode  表单模板编号（与应用绑定 formCode 一致），不可为空
     * @param rightId       CAP4 操作权限 ID，不可为空
     * @param beginDateTime 导出起始日期，格式 yyyy-MM-dd，不可为空
     * @param endDateTime   导出截止日期，格式 yyyy-MM-dd，不可为空
     * @param dataId        指定主表数据 ID，可空
     * @param page          页码，可空
     * @param pageSize      每页条数，可空（联调建议 1，减少业务数据量）
     * @return 可直接 POST 至 {@code /seeyon/rest/cap4/form/soap/export} 的请求体
     */
    public static Map<String, Object> toExportBody(String templateCode,
                                                   String rightId,
                                                   String beginDateTime,
                                                   String endDateTime,
                                                   Long dataId,
                                                   Integer page,
                                                   Integer pageSize) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("templateCode", templateCode.trim());
        body.put("rightId", rightId.trim());
        body.put("beginDateTime", beginDateTime.trim());
        body.put("endDateTime", endDateTime.trim());
        if (dataId != null && dataId > 0) {
            body.put("dataId", dataId);
        }
        if (page != null && page > 0) {
            body.put("page", page);
        }
        if (pageSize != null && pageSize > 0) {
            body.put("pageSize", pageSize);
        }
        return body;
    }
}
