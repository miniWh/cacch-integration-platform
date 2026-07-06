package com.cacch.integration.manager.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComDeleteFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordWriteItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComFieldAddItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComFieldUpdateItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateFieldsResponse;
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
     * 添加智能表格子表
     */
    WeComAddSheetResponse addSheet(String docId, String title, Integer index);

    /**
     * 更新智能表格子表标题
     */
    WeComUpdateSheetResponse updateSheet(String docId, String sheetId, String title);

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

    /**
     * 向智能表格子表添加记录（使用配置中的自建应用鉴权）
     *
     * @param docId   文档 docid
     * @param sheetId 子表 ID
     * @param records 待写入记录列表
     * @return 添加结果（含新 recordId）
     */
    WeComAddRecordsResponse addRecords(String docId, String sheetId, List<WeComRecordWriteItem> records);

    /**
     * 更新智能表格子表记录（使用配置中的自建应用鉴权）
     *
     * @param docId   文档 docid
     * @param sheetId 子表 ID
     * @param records 待更新记录列表（须含 recordId）
     * @return 更新结果
     */
    WeComUpdateRecordsResponse updateRecords(String docId, String sheetId, List<WeComRecordWriteItem> records);

    /**
     * 添加智能表格字段
     *
     * @param docId   文档 docid
     * @param sheetId 子表 ID
     * @param fields  待添加字段列表
     * @return 添加结果（含 fieldId）
     */
    WeComAddFieldsResponse addFields(String docId, String sheetId, List<WeComFieldAddItem> fields);

    /**
     * 删除智能表格字段
     */
    WeComDeleteFieldsResponse deleteFields(String docId, String sheetId, List<String> fieldIds);

    /**
     * 更新智能表格字段（重命名列等）
     *
     * @param docId   文档 docid
     * @param sheetId 子表 ID
     * @param fields  待更新字段列表
     * @return 更新结果
     */
    WeComUpdateFieldsResponse updateFields(String docId, String sheetId, List<WeComFieldUpdateItem> fields);
}
