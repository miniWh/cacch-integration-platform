package com.cacch.integration.controller.oa;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.oa.client.dto.OaFileDownloadResult;
import com.cacch.integration.service.oa.api.IOaAttachmentQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * NC 单号附件查询下载 REST 接口
 *
 * <p>对外提供按 NC 单号（致远 OA 内部购销合同表 formmain_5296.field0273）下载关联附件的能力：
 * 命中附件时代理返回文件流；未命中时返回空 data（正常业务分支，非异常）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oa/nc-attachments")
public class OaNcAttachmentController {

    private final IOaAttachmentQueryService oaAttachmentQueryService;

    /**
     * 按 NC 单号查询并下载第一条关联附件
     *
     * @param ncNo NC 单号，对应致远 OA 内部购销合同表 formmain_5296.field0273，不可为空
     * @return 命中附件时返回文件流（Content-Disposition 携带文件名，无文件名时回退 fileId）；
     * 未命中附件或附件文件标识为空时返回 JSON 空数据（{@code Result.success()}）
     * @throws com.cacch.integration.common.exception.BizException ncNo 为空或致远 OA 下载失败时抛出，
     *                                                             由全局异常处理器统一转换为 {@code Result} 返回
     */
    @GetMapping("/{ncNo}")
    public ResponseEntity<?> downloadByNcNo(@PathVariable String ncNo) {
        Optional<OaFileDownloadResult> resultOpt = oaAttachmentQueryService.downloadAttachmentByNcNo(ncNo);
        if (resultOpt.isEmpty()) {
            log.info("【NC附件】按NC单号下载返回空, ncNo={}", ncNo);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.success());
        }
        OaFileDownloadResult result = resultOpt.get();
        String fileName = StringUtils.hasText(result.fileName()) ? result.fileName().trim() : ncNo.trim();
        String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
        MediaType contentType = resolveDownloadMediaType(result.contentType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(contentType)
                .contentLength(result.contentLength())
                .body(new ByteArrayResource(result.content()));
    }

    /**
     * 解析下载响应 Content-Type，非法或缺失时回退 application/octet-stream
     *
     * @param contentType OA 响应 MIME 类型，可空
     * @return 可用的 MediaType，不为 null
     */
    private MediaType resolveDownloadMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType.trim());
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
