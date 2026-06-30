package com.cacch.integration.integration.wecom.client.dto.doc;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企微 — 新建文档/智能表格响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComCreateDocResponse extends WeComBaseResponse {

    private String url;

    private String docid;
}
