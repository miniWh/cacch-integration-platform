package com.cacch.integration.service.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordWriteItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComFieldAddItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComFieldUpdateItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateRecordsResponse;

import java.util.List;

/**
 * 企业微信智能表格服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComSmartSheetService {

    /**
     * 查询智能表格子表
     *
     * @param accessToken       企微 access_token
     * @param docId             文档 docid
     * @param sheetId           可选，指定子表 ID
     * @param needAllTypeSheet  可选，是否返回全部类型子表
     */
    WeComGetSheetResponse getSheets(String accessToken, String docId, String sheetId, Boolean needAllTypeSheet);

    /**
     * 查询智能表格字段
     *
     * @param accessToken 企微 access_token
     * @param docId       文档 docid
     * @param sheetId     子表 ID
     * @param offset      偏移量
     * @param limit       分页大小
     */
    WeComGetFieldsResponse getFields(String accessToken, String docId, String sheetId, Integer offset, Integer limit);

    /**
     * 查询智能表格记录
     *
     * @param accessToken 企微 access_token
     * @param docId       文档 docid
     * @param sheetId     子表 ID
     * @param offset      偏移量
     * @param limit       分页大小
     */
    WeComGetRecordsResponse getRecords(String accessToken, String docId, String sheetId, Integer offset, Integer limit);

    /**
     * 添加智能表格记录
     */
    WeComAddRecordsResponse addRecords(String accessToken, String docId, String sheetId,
                                       List<WeComRecordWriteItem> records);

    /**
     * 更新智能表格记录
     */
    WeComUpdateRecordsResponse updateRecords(String accessToken, String docId, String sheetId,
                                               List<WeComRecordWriteItem> records);

    /**
     * 添加智能表格字段
     *
     * @param accessToken 企微 access_token
     * @param docId       文档 docid
     * @param sheetId     子表 ID
     * @param fields      待添加字段列表
     */
    WeComAddFieldsResponse addFields(String accessToken, String docId, String sheetId,
                                     List<WeComFieldAddItem> fields);

    /**
     * 更新智能表格字段（重命名列等）
     *
     * @param accessToken 企微 access_token
     * @param docId       文档 docid
     * @param sheetId     子表 ID
     * @param fields      待更新字段列表
     */
    WeComUpdateFieldsResponse updateFields(String accessToken, String docId, String sheetId,
                                             List<WeComFieldUpdateItem> fields);
}
