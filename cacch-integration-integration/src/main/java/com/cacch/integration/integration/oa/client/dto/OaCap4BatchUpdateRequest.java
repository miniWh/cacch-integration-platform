package com.cacch.integration.integration.oa.client.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 致远 CAP4 无流程 batch-update 请求体组装
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaCap4BatchUpdateRequest {

    private OaCap4BatchUpdateRequest() {
    }

    /**
     * 组装国内登记报告资料附件绑定请求体
     *
     * <p>通过 {@code attachmentInfos} 将已上传的 {@code fileUrl} 绑定到子表附件字段 {@code subReference}。</p>
     *
     * @param formCode        无流程表单模板编码，不可为空
     * @param loginName       具备底表操作权限的 OA 登录名，不可为空
     * @param rightId         CAP4 操作权限 ID，不可为空
     * @param doTrigger       是否执行触发器
     * @param formMainTable   主表物理名，如 formmain_4070
     * @param formMainId      主表行 ID
     * @param formSubTable    子表物理名，如 formson_5464
     * @param subRowId        子表行 ID
     * @param attachmentField 附件字段名，如 field0218
     * @param subReference    附件控件 subReference（写入 field0218）
     * @param fileUrl         REST 上传返回的文件 ID
     * @param sort            附件排序，从 1 起
     * @return 可直接 POST 至 {@code /seeyon/rest/cap4/form/soap/batch-update} 的请求体
     */
    public static Map<String, Object> regReportAttachmentBind(String formCode,
                                                              String loginName,
                                                              String rightId,
                                                              boolean doTrigger,
                                                              String formMainTable,
                                                              long formMainId,
                                                              String formSubTable,
                                                              long subRowId,
                                                              String attachmentField,
                                                              String subReference,
                                                              String fileUrl,
                                                              int sort) {
        Map<String, Object> attachmentFieldEntry = fieldEntry(attachmentField, subReference, "");

        Map<String, Object> subRecord = new LinkedHashMap<>();
        subRecord.put("id", subRowId);
        subRecord.put("fields", List.of(attachmentFieldEntry));

        Map<String, Object> subTable = new LinkedHashMap<>();
        subTable.put("name", formSubTable);
        subTable.put("records", List.of(subRecord));
        subTable.put("changedFields", List.of(attachmentField));

        Map<String, Object> masterRecord = new LinkedHashMap<>();
        masterRecord.put("id", formMainId);
        masterRecord.put("fields", new ArrayList<Map<String, Object>>());

        Map<String, Object> masterTable = new LinkedHashMap<>();
        masterTable.put("name", formMainTable);
        masterTable.put("record", masterRecord);

        Map<String, Object> attachmentInfo = new LinkedHashMap<>();
        attachmentInfo.put("subReference", subReference);
        attachmentInfo.put("fileUrl", fileUrl);
        attachmentInfo.put("sort", sort);

        Map<String, Object> dataItem = new LinkedHashMap<>();
        dataItem.put("masterTable", masterTable);
        dataItem.put("subTables", List.of(subTable));
        dataItem.put("attachmentInfos", List.of(attachmentInfo));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("formCode", formCode);
        body.put("loginName", loginName);
        body.put("rightId", rightId);
        body.put("doTrigger", doTrigger);
        body.put("dataList", List.of(dataItem));
        return body;
    }

    private static Map<String, Object> fieldEntry(String name, String value, String showValue) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("showValue", showValue);
        return field;
    }
}
