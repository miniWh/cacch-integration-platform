package com.cacch.integration.manager.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocResponse;

import java.util.List;

/**
 * 企业微信文档编排接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComDocManager {

    /**
     * 新建智能表格文档（内部自动获取 access_token）
     *
     * @param docName    文档名称
     * @param adminUsers 文档管理员 userid 列表
     * @return 新建文档响应
     */
    WeComCreateDocResponse createSmartSheetDoc(String docName, List<String> adminUsers);
}
