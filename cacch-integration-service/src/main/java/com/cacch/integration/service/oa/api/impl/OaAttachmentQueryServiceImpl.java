package com.cacch.integration.service.oa.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.oa.client.OaAttachmentDbClient;
import com.cacch.integration.integration.oa.client.OaClient;
import com.cacch.integration.integration.oa.client.dto.OaAttachmentRow;
import com.cacch.integration.integration.oa.client.dto.OaFileDownloadResult;
import com.cacch.integration.service.oa.api.IOaAttachmentQueryService;
import com.cacch.integration.service.oa.api.IOaTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * OA NC 编号附件查询下载服务实现
 *
 * <p>编排：联合查询 OA 只读库 → 无附件短路返回 empty → 按文件标识调致远 OA 下载接口。
 * 纯只读查询与 HTTP 调用，不涉及数据库写操作，不使用数据库事务。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaAttachmentQueryServiceImpl implements IOaAttachmentQueryService {

    private static final String BIZ = "NC附件";

    private final OaAttachmentDbClient oaAttachmentDbClient;
    private final IOaTokenService oaTokenService;
    private final OaClient oaClient;

    @Override
    public Optional<OaFileDownloadResult> downloadAttachmentByNcNo(String ncNo) {
        if (!StringUtils.hasText(ncNo)) {
            log.info("【{}】下载终止, reason=ncNo为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "ncNo 不能为空");
        }
        Optional<OaAttachmentRow> attachmentOpt = oaAttachmentDbClient.findFirstAttachmentByNcNo(ncNo);
        if (attachmentOpt.isEmpty()) {
            log.info("【{}】下载终止, reason=NC单号无匹配附件, ncNo={}", BIZ, ncNo.trim());
            return Optional.empty();
        }
        OaAttachmentRow attachment = attachmentOpt.get();
        if (!StringUtils.hasText(attachment.fileUrl())) {
            log.info("【{}】下载终止, reason=附件文件标识为空, ncNo={}, attachmentId={}",
                    BIZ, ncNo.trim(), attachment.id());
            return Optional.empty();
        }
        String token = oaTokenService.getToken(null);
        try {
            OaFileDownloadResult result = oaClient.downloadAttachmentFile(token, attachment.fileUrl().trim());
            log.info("【{}】下载完成, ncNo={}, attachmentId={}, fileName={}, byteLength={}",
                    BIZ, ncNo.trim(), attachment.id(), result.fileName(), result.contentLength());
            return Optional.of(result);
        } catch (RestClientException e) {
            log.info("【{}】下载终止, ncNo={}, fileId={}, reason={}",
                    BIZ, ncNo.trim(), attachment.fileUrl().trim(), e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "致远 OA 下载附件失败: " + e.getMessage(), e);
        }
    }
}
