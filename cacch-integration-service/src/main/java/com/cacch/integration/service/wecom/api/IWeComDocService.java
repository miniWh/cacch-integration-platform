package com.cacch.integration.service.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocResponse;

import java.util.List;

/**
 * 企业微信文档服务接口
 */
public interface IWeComDocService {

    /**
     * 新建智能表格文档
     *
     * @param accessToken 企微 access_token
     * @param docName     文档名称
     * @param adminUsers  文档管理员 userid 列表
     */
    WeComCreateDocResponse createSmartSheetDoc(String accessToken, String docName, List<String> adminUsers);
}
