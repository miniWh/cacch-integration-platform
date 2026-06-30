package com.cacch.integration.manager.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocResponse;

import java.util.List;

/**
 * 企业微信文档编排接口
 */
public interface IWeComDocManager {

    WeComCreateDocResponse createSmartSheetDoc(String docName, List<String> adminUsers);
}
