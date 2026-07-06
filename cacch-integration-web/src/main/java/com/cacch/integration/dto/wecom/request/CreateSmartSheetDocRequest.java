package com.cacch.integration.dto.wecom.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 新建企微智能表格文档请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class CreateSmartSheetDocRequest {

    /**
     * 文档名称
     */
    @NotBlank
    private String docName;

    /**
     * 文档管理员 userid 列表（至少一名）
     */
    @NotEmpty
    private List<@NotBlank String> adminUsers;
}
