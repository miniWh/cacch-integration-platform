package com.cacch.integration.integration.wecom.client.dto.doc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微 — 新建文档/智能表格请求
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComCreateDocRequest {

    private String spaceid;

    private String fatherid;

    @JsonProperty("doc_type")
    private Integer docType;

    @JsonProperty("doc_name")
    private String docName;

    @JsonProperty("admin_users")
    private List<String> adminUsers;
}
