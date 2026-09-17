package com.cacch.integration.service.oa.api;

import com.cacch.integration.integration.oa.client.dto.OaFileDownloadResult;

import java.util.Optional;

/**
 * OA NC 单号附件查询下载服务
 *
 * <p>按 NC 单号（内部购销合同表 formmain_5296.field0273）联合查询附件（OA 附件表 ctp_attachment），
 * 命中后调用致远 OA 附件下载接口代理返回文件流；未命中或文件标识为空返回 empty（正常业务分支）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaAttachmentQueryService {

    /**
     * 按 NC 单号查询并下载第一条关联附件
     *
     * @param ncNo NC 单号（对应内部购销合同表 field0273），不可为空
     * @return 附件下载结果（含文件名、MIME 类型与内容）；无匹配附件或附件文件标识为空时返回 empty
     */
    Optional<OaFileDownloadResult> downloadAttachmentByNcNo(String ncNo);
}
