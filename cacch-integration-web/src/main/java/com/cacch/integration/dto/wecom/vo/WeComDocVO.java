package com.cacch.integration.dto.wecom.vo;

import lombok.Data;

/**
 * 企微文档创建结果 VO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComDocVO {

    /**
     * 文档 docid
     */
    private String docId;

    /**
     * 文档访问链接
     */
    private String url;
}
