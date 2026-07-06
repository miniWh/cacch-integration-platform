package com.cacch.integration.manager.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordWriteItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateRecordsResponse;

import java.util.List;

/**
 * 企业微信智能表格编排接口（对外唯一入口）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComSmartSheetManager {

    /**
     * 查询智能表格子表（使用配置中的自建应用 corpid + secret 鉴权）
     *
     * @param docId             文档 docid
     * @param sheetId           可选，指定子表 ID
     * @param needAllTypeSheet  可选，是否返回全部类型子表
     */
    WeComGetSheetResponse getSheets(String docId, String sheetId, Boolean needAllTypeSheet);

    /**
     * 查询智能表格字段（使用配置中的自建应用 corpid + secret 鉴权）
     *
     * @param docId   文档 docid
     * @param sheetId 子表 ID
     * @param offset  偏移量
     * @param limit   分页大小
     */
    WeComGetFieldsResponse getFields(String docId, String sheetId, Integer offset, Integer limit);

    /**
     * 查询智能表格记录（使用配置中的自建应用 corpid + secret 鉴权）
     *
     * @param docId   文档 docid
     * @param sheetId 子表 ID
     * @param offset  偏移量
     * @param limit   分页大小
     */
    WeComGetRecordsResponse getRecords(String docId, String sheetId, Integer offset, Integer limit);

    WeComAddRecordsResponse addRecords(String docId, String sheetId, List<WeComRecordWriteItem> records);

    WeComUpdateRecordsResponse updateRecords(String docId, String sheetId, List<WeComRecordWriteItem> records);
}
